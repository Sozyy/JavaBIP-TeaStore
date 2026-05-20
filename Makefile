.PHONY: all clean run

# Variables - centralisées pour éviter les divergences
INPUT_MM   = metamodels/chips1.1.ecore
OUTPUT_MM  = metamodels/JavaBIPv0.2.ecore
INPUT_XMI  = models/TeaStoreVariation.xmi
OUTPUT_XMI = models/out.xmi
ATL        = transformation/chips_to_javabip.atl

all: emftvm.java
	javac -cp "lib/*" emftvm.java

clean:
	rm -f *.class transformation/*.emftvm

run:
	./emftvm.sh $(INPUT_MM) $(OUTPUT_MM) $(INPUT_XMI) $(ATL) $(OUTPUT_XMI)