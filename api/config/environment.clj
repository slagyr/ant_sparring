(use 'joodo.env)

(def environment {
  :joodo.core.namespace "ants.api.core"
  ; environment settings go here
  })

(swap! *env* merge environment)