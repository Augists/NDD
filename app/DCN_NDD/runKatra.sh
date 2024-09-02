mvn clean
clear
mvn compile
cd target/classes/
# java -cp ./ apkeep.katra.DCNKatraNetwork APKeep_6 -Xmx32768m
# java -cp ./ apkeep.katra.DCNKatraNetwork APKeep_10 -Xmx32768m
# java -cp ./ apkeep.katra.DCNKatraNetwork DCN_xjtu_20 -Xmx32768m
# java -cp ./ apkeep.katra.DCNKatraNetwork DCN_xjtu_50 -Xmx32768m
# java -cp ./ apkeep.katra.DCNKatraNetwork DCN_xjtu_100 -Xmx32768m
# java -cp ./ apkeep.katra.DCNKatraNetwork DCN_xjtu_200 -Xmx32768m
# java -cp ./ apkeep.katra.DCNKatraNetwork APKeep_500 -Xmx32768m

java -cp ./ apkeep.katra.NetworkKatra -Xmx32768m

cd ..
cd ..