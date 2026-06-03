# Chips_to_JavaBIP

Tool to transform a CHIPS model in the form of an XMI file into a JavaBIP project.

## Usage

Before running, make sure that the emftvm class file is present, otherwise, you can generate it by running 'make all' in the root of the project.

To generate the JavaBIP translation of the CHIPS example as an XMI file, simply run 'make run'. This will generate the 'out.xmi' located in the models folder.

To generate a JavaBIP project for the given example, just run 'make chips_to_javabip' and the generated Java files will be in the 'generated' folder. 