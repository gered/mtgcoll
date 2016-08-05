(defproject mtgcoll "1.1.2-SNAPSHOT"
  :description  "Magic: The Gathering card database and personal collection management web app."
  :url          "https://github.com/gered/mtgcoll"
  :license      {:name "MIT License"
                 :url  "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [compojure "1.4.0"]
                 [org.immutant/web "2.1.4"]

                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.postgresql/postgresql "9.4.1208.jre7"]
                 [com.taoensso/sente "1.8.1"]
                 [gered/views "1.5"]
                 [gered/views.sql "0.1"]
                 [gered/views.reagent "0.1"]
                 [gered/views.reagent.sente "0.1"]
                 [gered/webtools "0.1.1"]
                 [gered/webtools.reagent "0.1.1"]
                 [hiccup "1.0.5"]
                 [reagent "0.6.0-alpha2"]
                 [secretary "1.2.3"]
                 [clj-http "2.2.0"]
                 [honeysql "0.7.0"]
                 [ring-middleware-format "0.7.0"]
                 [ragtime "0.6.0"]
                 [enlive "1.1.6"]
                 [slugger "1.0.1"]
                 [cheshire "5.6.1"]
                 [cljsjs/chartjs "2.0.1-0"]
                 [cljsjs/showdown "1.4.2-0"]
                 [luminus/ring-ttl-session "0.3.1"]

                 [gered/config "0.1"]]

  :plugins       [[lein-cljsbuild "1.1.3"]
                  [lein-figwheel "0.5.4-1"]]

  :main          mtgcoll.core

  :repl-options  {:init-ns user}

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :main :compiler :output-dir]
                                    [:cljsbuild :builds :main :compiler :output-to]]
  :cljsbuild     {:builds {:main
                           {:source-paths ["src"]
                            :figwheel     true
                            :compiler     {:main          mtgcoll.client.core
                                           :output-to     "resources/public/cljs/app.js"
                                           :output-dir    "resources/public/cljs/target"
                                           :asset-path    "cljs/target"
                                           :source-map    true
                                           :optimizations :none
                                           :pretty-print  true}}}}

  :profiles      {:dev     {:source-paths   ["env/dev/src"]
                            :resource-paths ["env/dev/resources"]
                            :dependencies   [[pjstadig/humane-test-output "0.8.0"]]
                            :injections     [(require 'pjstadig.humane-test-output)
                                             (pjstadig.humane-test-output/activate!)]
                            :cljsbuild      {:builds {:main {:source-paths ["env/dev/src"]}}}}

                  :uberjar {:env       {}
                            :aot       :all
                            :hooks     [leiningen.cljsbuild]
                            :cljsbuild {:jar      true
                                        :figwheel false
                                        :builds   {:main
                                                   {:compiler ^:replace {:output-to     "resources/public/cljs/app.js"
                                                                         :optimizations :advanced
                                                                         :pretty-print  false}}}}}}

  :aliases       {"uberjar"  ["do" ["clean"] ["uberjar"]]
                  "cljsdev"  ["do" ["cljsbuild" "once"] ["cljsbuild" "auto"]]
                  "migrate"  ["run" "-m" "user/migrate"]
                  "rollback" ["run" "-m" "user/rollback"]})
