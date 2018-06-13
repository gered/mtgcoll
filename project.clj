(defproject mtgcoll "1.2.1"
  :description  "Magic: The Gathering card database and personal collection management web app."
  :url          "https://github.com/gered/mtgcoll"
  :license      {:name "MIT License"
                 :url  "http://opensource.org/licenses/MIT"}

  :dependencies [[cheshire "5.6.1"]
                 [clj-http "3.8.0"]
                 [cljsjs/chartjs "2.0.1-0"]
                 [cljsjs/showdown "1.4.2-0"]
                 [com.taoensso/sente "1.8.1"]
                 [compojure "1.6.0"]
                 [enlive "1.1.6"]
                 [gered/config "0.1"]
                 [gered/views "1.5"]
                 [gered/views.reagent "0.1"]
                 [gered/views.reagent.sente "0.1"]
                 [gered/views.sql "0.1"]
                 [gered/webtools "0.1.1"]
                 [gered/webtools.reagent "0.1.1"]
                 [hiccup "1.0.5"]
                 [honeysql "0.7.0"]
                 [mount "0.1.12"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [org.immutant/web "2.1.9"]
                 [org.postgresql/postgresql "9.4.1208.jre7"]
                 [org.webjars/bootstrap "3.3.6"]
                 [ragtime "0.6.0"]
                 [reagent "0.6.0"]
                 [ring "1.6.3"]
                 [ring-middleware-format "0.7.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-defaults "0.3.1" :exclusions [javax.servlet/servlet-api]]
                 [secretary "1.2.3"]
                 [slugger "1.0.1"]]

  :plugins       [[lein-cljsbuild "1.1.7"]]

  :main          mtgcoll.core

  :repl-options  {:init-ns user}

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :main :compiler :output-dir]
                                    [:cljsbuild :builds :main :compiler :output-to]]

  :cljsbuild     {:builds
                  {:main
                   {:source-paths ["src"]
                    :compiler     {:asset-path    "cljs/target"
                                   :main          mtgcoll.client.core
                                   :optimizations :none
                                   :output-dir    "resources/public/cljs/target"
                                   :output-to     "resources/public/cljs/app.js"
                                   :pretty-print  true
                                   :source-map    true}}}}

  :profiles      {:dev     {:source-paths   ["profiles/dev/src"]
                            :resource-paths ["profiles/dev/resources"]
                            :dependencies   [[binaryage/devtools "0.9.4"]
                                             [pjstadig/humane-test-output "0.8.0"]]
                            :injections     [(require 'pjstadig.humane-test-output)
                                             (pjstadig.humane-test-output/activate!)]
                            :cljsbuild      {:builds
                                             {:main
                                              {:source-paths ["profiles/dev/src"]
                                               :compiler     {:preloads [devtools.preload]}}}}}

                  :uberjar {:source-paths   ["profiles/uberjar/src"]
                            :resource-paths ["profiles/uberjar/resources"]
                            :aot            :all
                            :hooks          [leiningen.cljsbuild]
                            :omit-source    true
                            :cljsbuild      {:jar      true
                                             :builds   {:main
                                                        {:compiler ^:replace {:optimizations :advanced
                                                                              :output-to     "resources/public/cljs/app.js"
                                                                              :pretty-print  false}}}}}}

  :aliases       {"rundev"   ["run" "--" "--config" "my-config.edn"]
                  "uberjar"  ["do" ["clean"] ["uberjar"]]
                  "cljsdev"  ["do" ["cljsbuild" "once"] ["cljsbuild" "auto"]]
                  "migrate"  ["run" "-m" "user/migrate" "--" "--config" "my-config.edn"]
                  "rollback" ["run" "-m" "user/rollback" "--" "--config" "my-config.edn"]})
