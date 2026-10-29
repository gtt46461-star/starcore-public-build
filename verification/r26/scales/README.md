Run the production pure-Java verification first, then the independent window oracle:

```sh
python3 scripts/verify_r26.py
mkdir -p build/r26/extra-tests
javac -encoding UTF-8 -cp build/r26/classes -d build/r26/extra-tests verification/r26/scales/ScaleWindowRetention.java
java -Xmx512m -cp build/r26/classes:build/r26/extra-tests com.ethk3.novaonepro.strategyv2.ScaleWindowRetention
```

The test compares the production model's sufficient statistics with counts recomputed from each complete observed window at 15 boundary points. It exercises 10,000 legal synthetic labels, all six declared scales, wrap of the bounded 5,000-label ring, restoration at label 5,017, exact subsequent state at 6,000/10,000, and gap rebuilding while keeping the fitted router.

This is an additional host kernel regression. It does not test the whole app's raw database retention, natural prediction quality or device latency. The source-bound Android suite separately verifies persisted expert scale counters.
