(defproject me.shenfeng/netty-http "1.0.0-SNAPSHOT"
  :description "http client based on netty"
  :java-source-path "src/java"
  :warn-on-reflection true
  :repositories {"JBoss"
                 "http://repository.jboss.org/nexus/content/groups/public/"}
  :dependencies [[org.jboss.netty/netty "3.2.5.Final"]]
  :dev-dependencies [[ring/ring-jetty-adapter "0.3.11"]
                     [ring/ring-core "0.3.11"]
                     [org.clojure/clojure "1.2.1"]
                     [junit/junit "4.8.2"]
                     [ch.qos.logback/logback-classic "0.9.29"]])
