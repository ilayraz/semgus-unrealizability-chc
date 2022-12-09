import sys
from antlr4 import *
from SygusLexer import SygusLexer
from SygusParser import SygusParser
from spacerVisitor import SpacerVisitor
import glob, os

# read a sygus problem
# and an example file where the first line and the last column should be ignored
# python spacer.py fileName exampleFile
def main(argv):
    # read sygus file
    fileName_sygus = argv[1]
    file_sygus = open(fileName_sygus, "r")
    input = InputStream(file_sygus.read())
    file_sygus.close()
#    print( input )
    print( "get grammar from: "+ argv[1] )

    lexer = SygusLexer(input)
    print( "lexer:" + lexer )

    stream = CommonTokenStream(lexer)
    parser = SygusParser(stream)
    tree = parser.cmdPlus()

    print( "get examples from: "+ argv[2] )

    # example file
    fileName_example = argv[2]
    file_example = open(fileName_example, "r")
    file_example.readline()
    lines = file_example.readlines()
    file_example.close()
    list_example = []
    for line in lines:
        list_example.append(line.split()[:-1])
    for line in lines:
        print(line.split()[:-1])

    v = SpacerVisitor()
    v.ex_list = list_example
    v.visit(tree) 
    print( v.header )
    print( v.spec )
    print( v.result )


    # todo initial v
    # read cex file

def sygus_with_example(argv):
    # read grammar file
    fileName_sygus = argv[1]
    file_sygus = open(fileName_sygus, "r")
    input = InputStream(file_sygus.read())
    file_sygus.close()
    print( input )
    print( "got grammar from: "+ argv[1] )

    lexer = SygusLexer(input)
    print( lexer )

    stream = CommonTokenStream(lexer)
    parser = SygusParser(stream)
    tree = parser.cmdPlus()
    # read example file
    fileName_example = argv[2]
    file_example = open(fileName_example, "r")
    file_example.readline()
    lines = file_example.readlines()
    file_example.close()
    list_example = []
    list_output = []
    for line in lines:
        list_output.append(line.split()[-1])
        list_example.append(line.split()[:-1])
    print( "got examples from: "+ argv[2] )
    print( list_example )
    print( list_output )
 
    v = SpacerVisitor()
    v.ex_list = list_example
    v.visit(tree) 
    result = v.header
    #print v.spec
    result = result + v.result
    result = result + "\n(assert (forall ("
    for i in range(0,len(list_example)):
        result = result + " (x_" + str(i) + " Int) "
    result = result + ")\n\t(=> (Start "
    for i in range(0,len(list_example)):
        result = result + " x_" + str(i) + " "
    result = result + ") (not (and\n"
    for i in range(0,len(list_example)):
        result = result + "\t\t(= x_" + str(i) + " " + list_output[i] + ")\n"

    result = result + ")))))\n(check-sat)"
    f = open("horn_query.smt", "w")
    f.write(result)
    f.close()

    print("------------------------------------------------")
    print(result)    


    # todo initial v
    # read cex file


if __name__ == '__main__':
    sygus_with_example(sys.argv)
