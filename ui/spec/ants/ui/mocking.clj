(ns ants.ui.mocking
  (:use
    [ants.ui.core]))

(deftype MockUI [update]
  AntsUI
  (update-ui [this data] (reset! update data)))

(deftype MockDataSource [stub urls]
  AntsDataSource
  (curl [this url] (swap! urls conj url) @stub))

(defn new-mock-ui []
  (MockUI. (atom nil)))

(defn new-mock-data-source []
  (MockDataSource. (atom nil) (atom [])))

(deftype MockInteractor [call params]
  AntsInteractor
  (startup [this host]
    (reset! call "startup")
    (reset! params {:host host}))
  (shutdown [this]
    (reset! call "shutdown"))
  (update [this]
    (reset! call "update")))

(defn new-mock-interactor []
  (MockInteractor. (atom nil) (atom nil)))