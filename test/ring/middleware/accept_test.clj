(ns ring.middleware.accept-test
	(:require ring.middleware.accept)
	(:use midje.sweet))

(tabular
	(fact
		(get-in ((ring.middleware.accept/wrap-accept identity {?key ?offered}) {:headers ?accept}) [:accept ?key]) => ?expected)
	?key        ?accept                               ?offered              ?expected
	:encoding   {"accept-encoding" "a"}               ["a"]                 "a"
	:encoding   {"accept-encoding" "*"}               ["a"]                 "a"
	:encoding   {"accept-encoding" "a"}               ["b"]                 nil
	:mime       {"accept" "a/a"}                      ["a/a"]               "a/a"
	:mime       {"accept" "a/a"}                      ["a/b"]               nil
	:mime       {"accept" "a/*"}                      ["a/a"]               "a/a"
	:mime       {"accept" "a/*"}                      ["b/b"]               nil
	:mime       {"accept" "*/*"}                      ["b/b"]               "b/b"
	:language   {"accept-language" "en-gb"}           ["en-gb"]             "en-gb"
	:language   {"accept-language" "en-gb"}           ["en-us"]             nil
	:language   {"accept-language" "en"}              ["en-gb"]             "en-gb"
	:language   {"accept-language" "en"}              ["fr-ca"]             nil
	:language   {"accept-language" "*"}               ["en-gb"]             "en-gb"
	:encoding   {"accept-encoding" "a,b;q=0.5"}       ["a" "b"]             "a"
	:encoding   {"accept-encoding" "a;q=0.5,b"}       ["a" "b"]             "b"
	:encoding   {"accept-encoding" "a;q=0"}           ["a"]                 nil
	:mime       {"accept" "a/a,a/*;q=0"}              ["a/a"]               "a/a"
	:mime       {"accept" "a/*,*/*;q=0"}              ["a/a"]               "a/a"
	:mime       {"accept" "a/a,a/*;q=0"}              ["a/b"]               nil
	:encoding   {"accept-encoding" "a"}               ["a" :as :a]          :a
	:encoding   {"accept-encoding" "b"}               ["a" :as :a]          nil
	:encoding   {"accept-encoding" "a,b;q=0.5"}       ["a" :qs 0.1, "b"]    "b"
	:encoding   {"accept-encoding" "a;q=0.5,b"}       ["a", "b" :qs 0.1]    "a"
	:encoding   {"accept-encoding" "a"}               ["a" :qs 0]           nil
	:encoding   {}                                    ["identity"]          "identity"
	:encoding   {"accept-encoding" "a"}               ["identity"]          "identity"
	:encoding   {"accept-encoding" "identity;q=0"}    ["identity"]          nil
	:encoding   {"accept-encoding" "*;q=0"}           ["identity"]          nil
	:charset    {"accept-charset" "a;q=0.9"}          ["iso-8859-1", "a"]   "iso-8859-1"
	:charset    {}                                    ["iso-8859-1"]        "iso-8859-1"
	:charset    {"accept-charset" "iso-8859-1;q=0"}   ["iso-8859-1"]        nil
	:charset    {"accept-charset" "*;q=0"}            ["iso-8859-1"]        nil
	:charset    {"accept-charset" ""}                 ["iso-8859-1"]        "iso-8859-1"

  :mime       {"accept" "a/a,b/b"}                  ["a/a" "b/b"]         "a/a"
  :mime       {"accept" "a/a,b/b"}                  ["b/b" "a/a"]         "a/a"
  :mime       {"accept" "a/a,b/b,c/c"}              ["c/c" "b/b" "a/a"]   "a/a"

  :mime       {"accept" "b/b,a/a"}                  ["a/a" "b/b"]         "a/a"
  :mime       {"accept" "b/b,a/a"}                  ["b/b" "a/a"]         "a/a")
