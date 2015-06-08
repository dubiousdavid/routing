(ns routing.middleware)

(defn apply-req-mw [f]
  (fn [state]
    [nil (update-in state [:routing/request] f)]))

(defn cleanup [resp]
  (dissoc resp :routing/middleware))
