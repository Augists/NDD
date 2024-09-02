mvn clean
clear
mvn compile
cd target/classes/

java -cp ./ apkeep.KatraNDD.FDNNetwork APKeep_6 -Xmx32768m
java -cp ./ apkeep.KatraNDD.FDNNetwork APKeep_10 -Xmx32768m
java -cp ./ apkeep.KatraNDD.FDNNetwork DCN_xjtu_20 -Xmx32768m
java -cp ./ apkeep.KatraNDD.FDNNetwork DCN_xjtu_50 -Xmx32768m
java -cp ./ apkeep.KatraNDD.FDNNetwork DCN_xjtu_100 -Xmx32768m
java -cp ./ apkeep.KatraNDD.FDNNetwork DCN_xjtu_200 -Xmx32768m
java -cp ./ apkeep.KatraNDD.FDNNetwork APKeep_500 -Xmx32768m

# java -cp ./ apkeep.FDN.FDNNetwork -Xmx8192m
# /usr/bin/time -f "%M %t" java -cp ./ apkeep.FDN.FDNNetwork
# java -cp ./ apkeep.katra.DCNKatraNetwork -Xmx32768m

# java -cp ./ test

# java -cp ./ generateDataset.generateNat
cd ..
cd ..