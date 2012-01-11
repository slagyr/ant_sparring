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