(defproject listora/constraint "0.0.7-SNAPSHOT"
  :description "Data constraint library"
  :url "https://github.com/listora/constraint"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :deploy-repositories [["releases" :clojars]]

  :plugins [[lein-difftest "2.0.0"]
            [jonase/eastwood "0.1.4"]
            [lein-bikeshed "0.1.6"]]

  :aliases {"lint" ["do" ["bikeshed"] ["eastwood"]]
            "ci" ["do" ["difftest"] ["lint"]]})
