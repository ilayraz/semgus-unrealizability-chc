# Generated from Sygus.g4 by ANTLR 4.5.1
from antlr4 import *
import math

# This class defines a complete generic visitor for a parse tree produced by SygusParser.


class SpacerVisitor(ParseTreeVisitor):
    header = "(set-logic HORN)\n"
    result = ""
    spec = "(define-fun SPEC "
    num_ex = 0
    # var list of the synthFun
    varList = []
    spec_varList = []
    is_inSynthFunDef = False
    
    # name of the synthfun
    synthFun_name = ""

    reg_str = ""

    nt_sort_map = []
    is_inNtScan = False
    is_constraintStart = False

    application_args_in_gterm = []
    is_inApplication_inGterm = False

    # example list used to subtitute constraint
    ex_list = []

    current_nt = ""


    def get_application_def(self):
        result = "("
        for i in range(0,self.num_ex):
            for var in self.varList:
                result+=self.type + " " +var+"_"+str(i)+","
        result = result[:len(result)-1]+")"
        return  result



    def get_application(self):
        result = "("
        for i in range(0,self.num_ex):
            for var in self.varList:
                result+=var+"_"+str(i)+","
        result = result[:len(result)-1]+")"
        return  result

    def get_application_I(self,i):
        result = "("
        for var in self.varList:
            result+=var+"_"+str(i)+","
        result = result[:len(result)-1]+",I_"+str(i)+")"
        return  result

    def get_application_def_I(self):
        result ="("
        for var in self.varList:
            result+=self.type + " " +var+","
        result = result[:len(result)-1]+"," + self.type + " f)"
        return  result

    def get_application_spec(self):
        result ="("
        for var in self.spec_varList:
            result+=self.type + " " +var+","
        result = result[:len(result)-1]+"," + self.type + " f_OUT)"
        return  result


    # Visit a parse tree produced by SygusParser#start.
    def visitStart(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#prog.
    def visitProg(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#setLogicCmd.
    def visitSetLogicCmd(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#weightPlus.
    def visitWeightPlus(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#weight.
    def visitWeight(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#setWeightCmd.
    def visitSetWeightCmd(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#cmdPlus.
    def visitCmdPlus(self, ctx):
        self.num_ex = len(self.ex_list)
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#cmd.
    def visitCmd(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#weightOptimizationCmd.
    def visitWeightOptimizationCmd(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#weightPair.
    def visitWeightPair(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#weightConstraintCmd.
    def visitWeightConstraintCmd(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#varDeclCmd.
    def visitVarDeclCmd(self, ctx):
        self.spec_varList.append(ctx.SYMBOL().getText())
        self.spec += " ("+ctx.SYMBOL().getText()+" Int) "
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#sortDefCmd.
    def visitSortDefCmd(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#sortExpr.
    def visitSortExpr(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#boolConst.
    def visitBoolConst(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#enumConst.
    def visitEnumConst(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#ecList.
    def visitEcList(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#symbolPlus.
    def visitSymbolPlus(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#setOptsCmd.
    def visitSetOptsCmd(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#optList.
    def visitOptList(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#symbolPairPlus.
    def visitSymbolPairPlus(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#symbolPair.
    def visitSymbolPair(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#funDefCmd.
    def visitFunDefCmd(self, ctx):
        self.header = self.header + "(define-fun " + ctx.SYMBOL().getText() 
        self.reg_str = " "
        self.visit(ctx.argList())
        self.reg_str += ctx.sortExpr().getText() + " "
        self.visit(ctx.term())
        self.header += self.reg_str + ")\n"
        return ""


    # Visit a parse tree produced by SygusParser#funDeclCmd.
    def visitFunDeclCmd(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#sortStar.
    def visitSortStar(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#argList.
    def visitArgList(self, ctx):
        self.reg_str += "("
        self.visitChildren(ctx)
        self.reg_str += ") "
        return ""


    # Visit a parse tree produced by SygusParser#symbolSortPairStar.
    def visitSymbolSortPairStar(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#symbolSortPair.
    def visitSymbolSortPair(self, ctx):
        self.reg_str += " (" + ctx.SYMBOL().getText() + " " + ctx.sortExpr().getText()+")"
        if self.is_inSynthFunDef:
            self.varList.append(ctx.SYMBOL().getText())
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#application.
    def visitApplication(self, ctx):
        if self.is_constraintStart:
            if ctx.SYMBOL().getText() == self.synthFun_name:
                self.reg_str += self.synthFun_name
                return 1
        self.reg_str += "(" +ctx.SYMBOL().getText() + " "
        self.visitChildren(ctx)
        self.reg_str += ") " 
        return 1

    # Visit a parse tree produced by SygusParser#liter.
    def visitLiter(self, ctx):
        self.reg_str += " " + ctx.literal().getText() +" "
        return 1


    # Visit a parse tree produced by SygusParser#symbol.
    def visitSymbol(self, ctx):
        self.reg_str += " " +ctx.SYMBOL().getText() +" "
        return 1


    # Visit a parse tree produced by SygusParser#let.
    def visitLet(self, ctx):
        return 1

    # Visit a parse tree produced by SygusParser#letTerm.
    def visitLetTerm(self, ctx):
        return self.visit(ctx.term())


    # Visit a parse tree produced by SygusParser#letBindingTermPlus.
    def visitLetBindingTermPlus(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#letBindingTerm.
    def visitLetBindingTerm(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#termStar.
    def visitTermStar(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#literal.
    def visitLiteral(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#ntDefPlus.
    def visitNtDefPlus(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#ntDef.
    def visitNtDef(self, ctx):
        if self.is_inNtScan:
            self.nt_sort_map.append([ctx.SYMBOL().getText(),ctx.sortExpr().getText()])
            return ""

        self.current_nt = ctx.SYMBOL().getText()
        self.header += "(declare-fun " + ctx.SYMBOL().getText() + " ("
        for i in range(0, self.num_ex):
            self.header += ctx.sortExpr().getText() + " "
        self.header += ") Bool)\n"
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#gTermPlus.
    def visitGTermPlus(self, ctx):

        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#checkSynthCmd.
    def visitCheckSynthCmd(self, ctx):
        self.spec += "))\n"
        self.result += "(assert (forall ("
        for i in range(0,self.num_ex):
            self.result += " (x_"+str(i) + " Int)"
        self.result += ")\n (=> (Start "
        for i in range(0,self.num_ex):
            self.result += " x_"+str(i)
        self.result += ") (not (and\n"
        for i in range(0,self.num_ex):
            self.result += "\t(SPEC x_"+str(i)
            for j in range(0,len(self.varList)):
                self.result += " "+self.ex_list[i][j]
            self.result += ")\n"
        self.result += ")))))\n(check-sat)\n(get-model)"
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#constraintCmd.
    def visitConstraintCmd(self, ctx):
        if not self.is_constraintStart:
            self.spec += ") Bool\n (and "
            self.is_constraintStart = True
        self.reg_str = ""
        self.visitChildren(ctx)
        self.spec += self.reg_str
        return ""


    # Visit a parse tree produced by SygusParser#synthFunCmd.
    def visitSynthFunCmd(self, ctx):
        self.synthFun_name = ctx.SYMBOL().getText()
        self.spec += " (("+self.synthFun_name+" Int)"

        self.is_inNtScan = True
        self.visit(ctx.ntDefPlus())
        self.is_inNtScan = False

        self.is_inSynthFunDef = True
        self.visitChildren(ctx)
        self.is_inSynthFunDef = False

        return ""


    # Visit a parse tree produced by SygusParser#gTermWeighted.
    def visitGTermWeighted(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#literalPlus.
    def visitLiteralPlus(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#gTerm.
    def visitGTerm(self, ctx):
        if ctx.gTermStar() != None:
            print("gTermStar")

            self.is_inApplication_inGterm = True
            self.application_args_in_gterm = []
            self.visit(ctx.gTermStar())
            self.result += "(assert (forall ("
            for i in range(0,len(self.application_args_in_gterm)):
                for j in range(0,self.num_ex):
                    sort = ""
                    for k in range(0,len(self.nt_sort_map)):
                        if self.nt_sort_map[k][0] == self.application_args_in_gterm[i]:
                            sort = self.nt_sort_map[k][1]
                    self.result += "(x_"+str(i)+"_"+str(j)+" "+sort+")"+" "
            self.result += ")\n"
            self.result += " (=> (and "
            for i in range(0, len(self.application_args_in_gterm)):
                self.result += "(" + self.application_args_in_gterm[i]
                for j in range(0,self.num_ex):
                    self.result += " " + "x_" + str(i) + "_" + str(j)
                self.result += ") "
            self.result+=") ("+self.current_nt
            for j in range(0,self.num_ex):
                self.result += " ("+ctx.SYMBOL().getText()
                for i in range(0, len(self.application_args_in_gterm)):
                    self.result += " x_" + str(i) + "_" +str(j)
                self.result += ")"
            self.result += "))))\n\n"
            return ""

        if ctx.SYMBOL() != None:
            print("ctx.SYMBOL():"+ctx.SYMBOL().getText())
            if self.is_inApplication_inGterm:
                self.application_args_in_gterm.append(ctx.SYMBOL().getText())
                return ""

            self.result += "(assert (" + self.current_nt
            for i in range(0,len(self.varList)):
                if self.varList[i] == ctx.SYMBOL().getText():
                    for example in self.ex_list:
                        self.result += " " + example[i]
            self.result += "))\n"

        if ctx.literal() != None:
            print("ctx.literal()"+ctx.literal().getText())
            self.result += "(assert (" + self.current_nt
            for i in range(0,self.num_ex):
                self.result += " " + ctx.literal().getText()
            self.result += "))\n"
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#letGTerm.
    def visitLetGTerm(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#letBindingGTermPlus.
    def visitLetBindingGTermPlus(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#letBindingGTerm.
    def visitLetBindingGTerm(self, ctx):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SygusParser#gTermStar.
    def visitGTermStar(self, ctx):
        return self.visitChildren(ctx)

