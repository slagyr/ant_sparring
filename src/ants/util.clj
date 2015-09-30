(ns ants.util)

(defn resolve-var [var-sym]
  (let [ns-sym (symbol (namespace var-sym))
        var-sym (symbol (name var-sym))]
    (require ns-sym)
    (if-let [var (ns-resolve (the-ns ns-sym) var-sym)]
      var
      (throw (Exception. (str "No such var " (name ns-sym) "/" (name var-sym)))))))

(defn resolve-var-or-nil [var-sym]
  (try
    (resolve-var var-sym)
    (catch Exception e
      ;(log/error e)
      nil)))