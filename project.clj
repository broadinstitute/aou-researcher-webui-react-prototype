(defproject allofus.rp.webui "0.0.1"
  :dependencies
  [
   [dmohs/react "1.1.0+15.4.2-2"]
   [funcool/promesa "1.8.1"]
   [org.clojure/clojure "1.8.0"]
   [org.clojure/clojurescript "1.9.521"]
   ]
  :plugins [[lein-cljsbuild "1.1.5"] [lein-figwheel "0.5.10"]]
  :profiles {:dev
             {:dependencies [[binaryage/devtools "0.9.4"]]
              :target-path "resources/public/target"
              :clean-targets ^{:protect false} ["resources/public/target"]
              :cljsbuild
              {:builds
               {:client
                {:compiler
                 {:optimizations :none
                  :source-map true
                  :source-map-timestamp true
                  :output-dir "resources/public/target/build"
                  :output-to "resources/public/target/compiled.js"
                  :preloads [devtools.preload]
                  :external-config {:devtools/config {:features-to-install [:formatters :hints]}}}
                 :figwheel true}}}}
             :deploy {:cljsbuild
                      {:builds {:client {:compiler
                                         {:optimizations :advanced
                                          :pretty-print false}}}}}}
  :cljsbuild {:builds {:client {:source-paths ["src/cljs"]
                                :compiler {:main "webui.core"
                                           :asset-path "target/build"}}}})
