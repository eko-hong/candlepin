# Intro
Mead is a Maven-based build system for Brew.  It works by building your
project via Maven, rendering a RPM spec file from a template, and then packaging
everything as an RPM with the rendered spec file.

## Getting Started
Clone the RCM utility-scripts repository.  All the scripts you'll need are in
the `mead` directory.

## Maven Build
Mead will not just download JAR files willy-nilly.  You must first import them
into the buildroot.  You must have the maven-import and regen-repo permissions
in Brew.  You can see your permissions with `brew list-permissions --mine`

To import just one artifact

```
$ ./get-maven-artifacts group:artifactId:packaging:version
$ ./import-maven --tag YOUR_DEPENDENCIES_TAG ARTIFACT_FILES*
$ ./brew regen-repo YOUR_BUILDROOT
```

For example:
```
$ get-maven-artifacts org.apache:apache:pom:11
$ ./import-maven --tag candlepin-mead-rhel-6-deps apache-11*
$ ./brew regen-repo candlepin-mead-rhel-6-build
```

Importing everything one at a time is slow, so the fastest way is to run your
build against an empty local repository and send the build log to
`mead-load-build-dependencies`.

```
$ cat > my_settings.xml <<SETTINGS
<settings>
  <localRepository>/tmp/m2_repo/</localRepository>
</settings>
SETTINGS
$ mvn -B dependency:resolve-plugins deploy -Dmaven.test.skip=true -s settings.xml -DaltDeploymentRepository=local-output::default::file:///tmp/output | tee /tmp/build.log
$ ./mead-load-build-dependencies --tag YOUR_DEPENDENCIES_TAG /tmp/build.log
```

It is important to use an empty local repository so that all dependencies will
show up in the build log.  If you use a repository that already has some of
your dependencies, `mead-load-build-dependencies` won't pick them up (since
Maven doesn't download them) and you'll be stuck importing them one at a time.

The worst situation to get stuck in is a cycle where you import, build, hit a
missing dependency, import it, build, hit a missing dependency in the newly
imported dependency, etc.  Doing a local build against a completely empty local
repository allows you to avoid this scenario since Maven will download (and
therefore print a message in the build log) everything that is needed.

Once you have all your dependencies imported, you need to create a git
repository somewhere that Brew can access it since Mead builds pull the source
from a git repository.  However, github is not on the whitelist.  The easiest
solution is to set up a repository off of git.engineering.redhat.com like so:

```
$ ssh KERB_ID@file.rdu.redhat.com
$ mkdir public_git
$ cd public_git
$ git clone --bare git://github.com/candlepin/candlepin

```

With the source available, you're ready to actually try a build using Brew's
`maven-build` sub-command.

```
$ brew maven-build candlepin-mead-rhel-6-maven-candidate --scratch
"git://git.engineering.redhat.com/users/KERB_ID/candlepin.git#REF_NAME"
```

`REF_NAME` can be any git ref.  I frequently run my build like

```
$ brew maven-build candlepin-mead-rhel-6-maven-candidate --scratch "git://git.engineering.redhat.com/users/awood/candlepin.git#$(git
 rev-parse HEAD)"
```

so the `git rev-parse` will expand out to the hash of the last commit on the
branch.

The `maven-build` command can take several different arguments to tweak the
build process so be sure to check out the help message.

## Spec File Template Rendering
Once a Maven build is complete, Mead can take a
[Cheetah](http://www.cheetahtemplate.org) of a RPM spec file use it to create
an actual RPM for the build.

A few special variables are made available to the Cheetah template.

* `$artifacts` - A hash of the Maven generated artifacts with the file
  extensions as keys.  For example: `{'.md5': 'my_project.jar.md5', '.jar':
  my_project.jar'}
* `$all_artifacts` - All Maven generated artifacts in a list (including MD5
  sums, pom files, etc).
* `$all_artifacts_with_path` - All artifacts but with the full path to the
  artifact within the project.
* `$version` - The version of the **top level** project.
* `$revision` - The revision of the **top_level** project.
* `$epoch` - The epoch of the **top_level** project.

Mead can also access files that are in dist-git such as patches.  However, it
cannot access anything in the lookaside cache (which is where the source
tarballs are normally kept).

You can test building an RPM from a template by using an existing Mead build
with the `mead-test-spec-fragment` script.

```
$ ./mead-test-spec-fragment --task-id 123456 server/candlepin.spec.tmpl
```

`mead-test-spec-fragment` also has some options to just render the template
without attempting an RPM build.

Other Cheetah notes:

* Dollar signs are meaningful in Cheetah.  If your spec file contains shell
  variables, you will need to surround the relevant block with `#raw` and `#end
  raw` tags.

  ```
  #raw
  for selinuxvariant in %{selinux_variants}
  do
    make NAME=$selinuxvariant -f /usr/share/selinux/devel/Makefile
    mv %{modulename}.pp %{modulename}.pp.$selinuxvariant
    make NAME=$selinuxvariant -f /usr/share/selinux/devel/Makefile clean
  done
  #end raw
  ```
* Do not use line continuations (i.e. ending a line with '\')!
  Cheetah doesn't like them for some reason.
* Do not use non-ASCII characters anywhere in the Cheetah template (including
  names with non-ASCII characters in the changelog.)  I ran into difficulties
  with the template failing to render when non-ASCII characters appeared in it.
* Always surround the entire `%changelog` section in `#raw` and `#end raw` tags
  so that people's changelog entries won't break the template.

## Putting It All Together
To build a wrapper-RPM all in one shot, you can use the `--specfile` option in
the `maven-build` subcommand.

```
$ brew maven-build candlepin-mead-rhel-6-maven-candidate --scratch --specfile "git://git.engineering.redhat.com/users/awood/candlepin.git?server#gutterball-1.0.17-1" "git://git.engineering.redhat.com/users/awood/candlepin.git#gutterball-1.0.17-1"
```

Notice in the `--specfile` option, I'm giving a URL to the **directory** with
the template in it as well as a pointer to the Git reference I want to build.

The `rhpkg` tool command is similar if you are working in dist-git, but you can
pass "." as an argument to `--specfile` and it will use the template checked-in
to dist-git.

```
$ rhpkg maven-build --scratch --sources="git://git.engineering.redhat.com/users/awood/candlepin.git#gutterball-1.0.17-1" --specfile=.  --scratch
```

`rhpkg maven-build` can also send options to Maven with `--maven-option` or
`--property`.  However, if you use `--maven-option`, use an equals sign to
connect it to the arguments so that `rhpkg` doesn't interpret the option that
should be going to Maven.  For example, `--maven-options='--debug'`.

## Gotchas
Maven and MEAD do not automatically handle building sibling projects.  For
example, Candlepin depends on Common.  Neither Maven nor Mead will
automatically build Common for you unless you build from the top.  If you
are building Candlepin and Common has been updated, you need to make sure to
run a MEAD build for Common first so that it will be in the repository.
