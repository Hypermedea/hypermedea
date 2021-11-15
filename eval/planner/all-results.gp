set xlabel "number of assembly steps"
set ylabel "execution time (in ms)"

set logscale y

set yrange [1:100000]

set style fill solid 0.25

plot "all-results.tsv" u 1:3:4 w filledcurve notitle lc 0, \
                    "" u 1:2 w linespoints t "FF" lc 1, \
                    "" u 1:6:7 w filledcurve notitle lc 0, \
                    "" u 1:5 w linespoints t "EYE" lc 2