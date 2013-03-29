(defproject me.shenfeng/async-http-client "1.1.0"
  :description "HTTP client based on netty"
  :java-source-paths ["src/java"]
  :url "https://github.com/shenfeng/async-http-client"
  :javac-options ["-source" "1.6" "-target" "1.6" "-g"]
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :warn-on-reflection true
  :repositories {"JBoss"
                 "http://repository.jboss.org/nexus/content/groups/public/"}
  :dependencies [[io.netty/netty "3.6.3.Final"]
                 [org.slf4j/slf4j-api "1.7.3"]]
  :profiles {:test {:java-source-paths ["test/java" "src/java"]}
             :dev {:dependencies [[ring/ring-jetty-adapter "0.3.11"]
                                  [com.google.guava/guava "10.0.1"]
                                  [ring/ring-core "0.3.11"]
                                  [org.clojure/clojure "1.4.0"]
                                  [compojure "1.1.5"]
                                  [ring/ring-jetty-adapter "1.1.8"]
                                  [ring/ring-core "1.1.8"]
                                  [junit/junit "4.8.2"]
                                  [ch.qos.logback/logback-classic "0.9.29"]]}})
