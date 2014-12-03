#! /usr/bin/env ruby
require 'net/http'
require 'webrick'
require 'webrick/https'

# Use the latest minitest
gem 'minitest'
require 'minitest/autorun'
require 'shoulda/context'

require '../candlepin'

# Minitest has two types of assertion: assert_x and must_x (e.g. assert_equals
# and must_equal) In these tests, we must use the assert_x type because the
# must_x types rely on Minitest setting a thread local, :current_spec, which
# won't work since these tests are multithreaded.

module Candlepin
  # Note that these are functional tests so they require
  # a running Candlepin.
  module Functional
    class TestClient < Minitest::Test
      context 'a client' do
        should 'get a status as JSON' do
          simple_client = NoAuthClient.new.raw_client
          res = simple_client.get('/status')
          assert(res.content.key?('version'))
        end

        should 'get owners with basic auth' do
          user_client = BasicAuthClient.new.raw_client
          res = user_client.get('/owners')
          refute_empty(res.content)
          assert(res.content.first.key?('id'))
        end

        should 'fail with bad password' do
          user_client = BasicAuthClient.new(:password => nil).raw_client
          res = user_client.get('/owners')
          assert_equal(401, res.status_code)
        end
      end
    end
  end

  module Unit
    class TestClientUnit < Minitest::Test
      TEST_PORT = 11999
      CLIENT_CERT_TEST_PORT = TEST_PORT + 1
      attr_accessor :server
      attr_accessor :client_cert_server

      context 'a client' do
        setup do
          key = OpenSSL::PKey::RSA.new(File.read('test-ca.key'))
          cert = OpenSSL::X509::Certificate.new(File.read('test-ca.cert'))

          server_config = {
            :BindAddress => 'localhost',
            :Port => TEST_PORT,
            :SSLEnable => true,
            :SSLPrivateKey => key,
            :SSLCertificate => cert,
            :Logger => WEBrick::BasicLog.new(nil, WEBrick::BasicLog::FATAL),
            :AccessLog => [],
          }

          @server = WEBrick::HTTPServer.new(server_config)
          @client_cert_server = WEBrick::HTTPServer.new(server_config.merge({
            :SSLVerifyClient => OpenSSL::SSL::VERIFY_PEER | OpenSSL::SSL::VERIFY_FAIL_IF_NO_PEER_CERT,
            :SSLCACertificateFile => 'test-ca.cert',
            :Port => CLIENT_CERT_TEST_PORT,
          }))

          [server, client_cert_server].each do |s|
            s.mount_proc('/candlepin/status') do |req, res|
              res.body = '{ "message": "Hello" }'
              res['Content-Type'] = 'text/json'
            end
          end

          @server_thread = Thread.new do
            server.start
          end

          @client_cert_server_thread = Thread.new do
            client_cert_server.start
          end
        end

        should 'use CA if given' do
          simple_client = NoAuthClient.new(
            :ca_path => 'test-ca.cert',
            :port => TEST_PORT,
            :insecure => false).raw_client

          res = simple_client.get('/status')
          assert_equal("Hello", res.content['message'])
        end

        should 'fail to connect if no CA given in strict mode' do
          simple_client = NoAuthClient.new(
            :port => TEST_PORT,
            :insecure => false).raw_client

          assert_raises(OpenSSL::SSL::SSLError) do
            simple_client.get('/status')
          end
        end

        should 'allow a connection with a valid client cert' do
          client_cert = OpenSSL::X509::Certificate.new(File.read('client.cert'))
          client_key = OpenSSL::PKey::RSA.new(File.read('client.key'))
          cert_client = ClientCertClient.new(
            :port => CLIENT_CERT_TEST_PORT,
            :ca_path => 'test-ca.cert',
            :insecure => false,
            :client_cert => client_cert,
            :client_key => client_key).raw_client

          res = cert_client.get('/status')
          assert_equal("Hello", res.content['message'])
        end

        should 'forbid a connection with an invalid client cert' do
          client_cert = OpenSSL::X509::Certificate.new(File.read('unsigned.cert'))
          client_key = OpenSSL::PKey::RSA.new(File.read('unsigned.key'))
          cert_client = ClientCertClient.new(
            :port => CLIENT_CERT_TEST_PORT,
            :ca_path => 'test-ca.cert',
            :insecure => false,
            :client_cert => client_cert,
            :client_key => client_key).raw_client

          e = assert_raises(OpenSSL::SSL::SSLError) do
            cert_client.get('/status')
          end
          assert_match(/unknown ca/, e.message)
        end

        teardown do
          server.shutdown
          client_cert_server.shutdown
          @server_thread.kill unless @server_thread.nil?
          @client_cert_server_thread.kill unless @client_cert_server_thread.nil?
        end
      end
    end
  end
end