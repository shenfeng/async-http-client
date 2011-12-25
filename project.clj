(defproject me.shenfeng/async-http-client "1.0.2"
  :description "http client based on netty"
  :java-source-path "src/java"
  :javac-options {:debug "true" :fork "true"}
  :warn-on-reflection true
  :repositories {"JBoss"
                 "http://repository.jboss.org/nexus/content/groups/public/"}
  :dependencies [[org.jboss.netty/netty "3.2.7.Final"]]
  :dev-dependencies [[ring/ring-jetty-adapter "0.3.11"]
                     [com.google.guava/guava "10.0.1"]
                     [ring/ring-core "0.3.11"]
                     [clojure "1.3.0"]
                     [junit/junit "4.8.2"]
                     [ch.qos.logback/logback-classic "0.9.29"]])
