(defproject me.shenfeng/async-http-client "1.0.0-SNAPSHOT"
  :description "http client based on netty"
  :java-source-path "src/java"
  :javac-options {:debug "true" :fork "true"}
  :warn-on-reflection true
  :repositories {"JBoss"
                 "http://repository.jboss.org/nexus/content/groups/public/"}
  :dependencies [[org.jboss.netty/netty "3.2.7.Final"]]
  :dev-dependencies [[ring/ring-jetty-adapter "0.3.11"]
                     [ring/ring-core "0.3.11"]
                     [junit/junit "4.8.2"]
                     [ch.qos.logback/logback-classic "0.9.29"]])
