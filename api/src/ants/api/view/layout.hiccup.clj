(doctype :html5)
[:html
 [:head
  [:meta {:http-equiv "Content-Type" :content "text/html" :charset "iso-8859-1"}]
  [:title "api"]
  (include-css "/stylesheets/api.css")
  (include-js "/javascript/api.js")]
 [:body
  (eval (:template-body joodo.views/*view-context*))
]]