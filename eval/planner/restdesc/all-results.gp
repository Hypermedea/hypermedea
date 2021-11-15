set xlabel "chain length"
set ylabel "execution time (in ms)"

set logscale x 2
set logscale y

set style fill solid 0.25

plot "all-results.tsv" u 1:5:6 w filledcurve notitle lc 0, \
                    "" u 1:2 w linespoints t "FF" lc 1, \
                    "" u 1:9:10 w filledcurve notitle lc 0, \
                    "" u 1:4 w linespoints t "EYE" lc 2, \
                    "" u 1:7:8 w filledcurve notitle lc 0, \
                    "" u 1:3 w linespoints t "PDDL4J (HSP)" lc 3