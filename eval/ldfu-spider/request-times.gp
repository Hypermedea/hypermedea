set datafile separator comma

set ylabel "elapsed time (ms)"
unset xtic

set style boxplot nooutliers
set yrange [0:65]

plot "request-times.csv" u (0):1 i 0 w boxplot t "Jena (RIOT)", \
                      "" u (1):1 i 1 w boxplot t "Linked-Data-Fu (nxparser)"