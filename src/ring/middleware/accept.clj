(ns ring.middleware.accept)

(defn- max-pos-key
	"like max-key but returns nil if no candidates are positive under score-fn"
	[cands score-fn]
	(when-not (empty? cands)
		(let [arg-max (apply max-key score-fn cands)]
			(if (pos? (score-fn arg-max))
				arg-max))))

(defn- match
	[offered prefs match-fn]
	(let [most-applicable-rule
			(fn [input]
				(max-pos-key prefs #(match-fn input (:name %))))]
		(if (seq offered)
			(let [result (max-pos-key offered #(* (:qs % 1) (:q (most-applicable-rule (:name %)) 0)))]
				(or (:as result) (:name result))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- exact-match
	[cand pattern]
	(if (or (= cand pattern) (= pattern "*")) 1 0))

(defn- mime-match
	[cand pattern]
	(reduce
		(fn [s [c p]] (cond (= p "*") s (= c p) (* s 2) :else 0)) ; award points for exact match but not * match
		1
		(map vector
			(clojure.string/split cand    #"/" 2)
			(clojure.string/split pattern #"/" 2))))

(defn- lang-match
	[cand pattern]
	(let
		[cand-len (count cand) pattern-len (count pattern)]
		(if
			(or (= cand pattern)
				(= pattern "*")
				(and (> cand-len pattern-len)
					(= (str pattern "-") (subs cand 0 (+ pattern-len 1)))))
			pattern-len 0))) ; prefer closer match

(defn- charset-post
	"If no * is present in an Accept-Charset field, then [...] ISO-8859-1 [...] gets a quality value of 1 if not explicitly mentioned.
	http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.2"
	[prefs]
	(if (and
			(not-any? #(= (:name %) "*") prefs)
			(not-any? #(= (:name %) "iso-8859-1") prefs))
		(conj prefs {:name "iso-8859-1" :q 1})
		prefs))

(defn- encoding-post
	"The identity content-coding is always acceptable, unless specifically refused
	http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.3"
	[prefs]
	(if (and
			(not-any? #(= (:name %) "*") prefs)
			(not-any? #(= (:name %) "identity") prefs))
		(conj prefs {:name "identity" :q 0.0009}) ; lower than any other encodings which were actually given (0.001 is the lowest permitted)
		prefs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- parse-accepts
	"Parse client capabilities and associated q-values"
	[accepts-string]
	(map
		(fn [[_ name q]]
			{:name name :q (Float/parseFloat (or q "1"))})
		(re-seq #"([^,;\s]+)[^,]*?(?:;\s*q=(0(?:\.\d{0,3})?|1(?:\.0{0,3})?))?" accepts-string)))

(defn- parse-offered
	"Parse offered types and associated source-qualities and aliases"
	[offered-list]
	(loop [res [] cur {} [a b :as unprocessed] offered-list]
		(cond
			(empty? unprocessed)
				(if (empty? cur) res (conj res cur))
			(some (partial = a) [:as :qs])
				(recur res (assoc cur a b) (drop 2 unprocessed))
			:else
				(recur (if (empty? cur) res (conj res cur)) {:name a} (rest unprocessed)))))

(defn wrap-accept
	[handler {:keys [mime charset encoding language]}]
	(let [match* (fn [offered accepts matcher-fn post-fn]
                 (match (parse-offered offered) (post-fn (parse-accepts accepts)) matcher-fn))
        assoc-in-once #(if (nil? (get-in %1 %2)) (assoc-in %1 %2 %3) %1)]
    (fn [{headers :headers :as request}]
      (println "headers" headers)
      (println "(headers \"accept\" \"*/*\")" (headers "accept" "*/*"))
      (-> request
          (assoc-in-once [:accept :mime]     (match* mime     (headers "accept" "*/*")               mime-match  identity))
          (assoc-in-once [:accept :charset]  (match* charset  (headers "accept-charset" "*")         exact-match charset-post))
          (assoc-in-once [:accept :encoding] (match* encoding (headers "accept-encoding" "identity") exact-match encoding-post))
          (assoc-in-once [:accept :language] (match* language (headers "accept-language" "*")        lang-match  identity))
          (handler)))))
