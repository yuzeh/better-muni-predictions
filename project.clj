(defproject better-muni-predictions "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-nextbus "0.1.0"]
                 [clojurewerkz/titanium "1.0.0-beta1"]]
  :main better-muni-predictions.core
  :profiles {:uberjar {:aot :all}})
