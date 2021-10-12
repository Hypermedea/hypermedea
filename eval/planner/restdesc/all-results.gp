set xlabel "chain length"
set ylabel "execution time (in ms)"

set logscale x 2
set logscale y

plot "all-results.tsv" u 1:2:5:6 w yerrorlines t "FF", \
                    "" u 1:4:9:10 w yerrorlines t "EYE", \
                    "" u 1:3:7:8 w yerrorlines t "PDDL4J (HSP)"