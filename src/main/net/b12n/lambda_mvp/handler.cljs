(ns net.b12n.lambda-mvp.handler
  "Demo handler: greet, echo the raw event, and report which invocation
  this warm sandbox is on. Same field shape as every other lambda-mvp-*
  sibling (message/runtime/request_id/warm_invocation/event), and the same
  code as lambda-mvp-cljs's handler apart from the context access below:
  only the compiler differs between the two projects.

  AWS's managed nodejs runtime parses the event into a JS object before
  calling this handler, so `event` is `js->clj`'d and rides back out
  through the return value, which the runtime JSON-serializes.")

(def warm-invocation-count (atom 0))

(defn handler
  "event: the already-parsed Lambda event (a JS object). context: the
  Lambda context object, whose awsRequestId is this invocation's request id.

  `(.-awsRequestId context)` is safe here, unlike under shadow-cljs's
  :advanced build, where lambda-mvp-cljs needs `unchecked-get`: Clojurific
  has no Closure Compiler, and its Vite plugin only renames the compiler's
  own `cljs$...` properties when it minifies, never a host object's."
  [event context]
  (let [n (swap! warm-invocation-count inc)]
    (clj->js {:message "hello from clojurific on lambda"
              :runtime "ClojureScript via Clojurific (Node.js)"
              :request_id (or (.-awsRequestId context) "")
              :warm_invocation n
              :event (js->clj event)})))
