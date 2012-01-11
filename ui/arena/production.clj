(use '[ants.ui.core])

(on-production-created [e]
  (let [ui (new-ants-ui (production e))
        ds (new-ants-data-source)]
    (backstage-put (production e) "interactor" (new-ants-interactor ui ds))))