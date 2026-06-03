.PHONY: all clean run

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

chips_to_javabip:
	./emftvm.sh $(INPUT_MM) $(OUTPUT_MM) $(INPUT_XMI) $(ATL) $(OUTPUT_XMI)
	python3 xmi_to_javabip.py $(OUTPUT_XMI) generated_javabip