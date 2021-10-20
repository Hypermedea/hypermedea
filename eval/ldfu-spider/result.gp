set datafile separator comma

set ylabel "execution time (ms)"
unset xtic

set style boxplot nooutliers

set yrange [0:6200]
set key bottom

set style fill pattern border -1

# FIXME: reasoner not called incrementally but once, at the end of navigation
plot "result.csv" u (0):2 i 0 w boxplot t "Hypermedea", \
               "" u (1):2 i 1 w boxplot t "Linked-Data-Fu", \
               "" u (2):($2+$3) i 0 w boxplot t "Hypermedea (w/ reasoning)", \
               "" u (3):($2+$3) i 1 w boxplot t "Linked-Data-Fu (w/ reasoning)"