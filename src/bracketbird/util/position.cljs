(ns bracketbird.util.position)


(defn absolut-position
  "Returns a map containing the aboslut left, top, width, height of
  the dom-element."
  [node]
  (let [p (.getBoundingClientRect node)
        left (.-left p)
        width (.-width p)
        height (.-height p)
        top (.-top p)

        w_height (.-innerHeight js/window)
        w_width (.-innerWidth js/window)]

    {:left          left :top top :right (- w_width (+ left width)) :bottom (- w_height (+ top height))
     :width         width :height height
     :window-width  w_width
     :window-height w_height}))


(defn offset-position
  "Returns a map containing the offset left, top, width, height of
  the dom-element."
  [node]
  {:left (.-offsetLeft node) :top (.-offsetTop node) :width (.-offsetWidth node) :height (.-offsetHeight node)})


(defn above?
  "returns true if postion p1's top is above postition p2's top."
  [p1 p2]
  (< (:top p1) (:top p2)))

(defn below?
  "returns true if postion p1's 'bottom' is below postition p2's 'bottom'."
  [p1 p2]
  (> (+ (:height p1) (:top p1)) (+ (:height p2) (:top p2))))


(defn scroll-into-view
  "Makes sure an item is visible in a scroll panel.

  item-node - the dom node that should be visible.
  panel-node - the surrounding scroll panel dom node."

  [item-node panel-node]
  (try
    (let [p-item (absolut-position item-node)
          p-panel (absolut-position panel-node)]

      (cond
        (above? p-item p-panel) (.scrollIntoView item-node true)
        (below? p-item p-panel) (.scrollIntoView item-node false)))
    (catch :default e (throw (js/Error. "unable to scroll into view - ref-item:" item-node "panel-ref:" panel-node e)))))
