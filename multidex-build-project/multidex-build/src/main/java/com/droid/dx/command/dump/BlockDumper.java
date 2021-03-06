/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.droid.dx.command.dump;

import com.droid.dx.cf.code.BasicBlocker;
import com.droid.dx.cf.code.ByteCatchList;
import com.droid.dx.cf.code.BytecodeArray;
import com.droid.dx.cf.code.Ropper;
import com.droid.dx.cf.iface.Method;
import com.droid.dx.rop.code.BasicBlock;
import com.droid.dx.rop.code.DexTranslationAdvice;
import com.droid.dx.util.Hex;
import com.droid.dx.util.IntList;
import java.io.PrintStream;

/**
 * Utility to dump basic block info from methods in a human-friendly form.
 */
public class BlockDumper
        extends BaseDumper {
    /** whether or not to registerize (make rop blocks) */
    private boolean rop;

    /**
     * {@code null-ok;} the class file object being constructed;
     * becomes non-null during {@link #dump}
     */
    protected com.droid.dx.cf.direct.DirectClassFile classFile;

    /** whether or not to suppress dumping */
    protected boolean suppressDump;

    /** whether this is the first method being dumped */
    private boolean first;

    /** whether or not to run the ssa optimziations */
    private boolean optimize;

    /**
     * Dumps the given array, interpreting it as a class file and dumping
     * methods with indications of block-level stuff.
     *
     * @param bytes {@code non-null;} bytes of the (alleged) class file
     * @param out {@code non-null;} where to dump to
     * @param filePath the file path for the class, excluding any base
     * directory specification
     * @param rop whether or not to registerize (make rop blocks)
     * @param args commandline parsedArgs
     */
    public static void dump(byte[] bytes, PrintStream out,
            String filePath, boolean rop, Args args) {
        BlockDumper bd = new BlockDumper(bytes, out, filePath,
                rop, args);
        bd.dump();
    }

    /**
     * Constructs an instance. This class is not publicly instantiable.
     * Use {@link #dump}.
     */
    BlockDumper(byte[] bytes, PrintStream out, String filePath,
            boolean rop, Args args) {
        super(bytes, out, filePath, args);

        this.rop = rop;
        this.classFile = null;
        this.suppressDump = true;
        this.first = true;
        this.optimize = args.optimize;
    }

    /**
     * Does the dumping.
     */
    public void dump() {
        byte[] bytes = getBytes();
        com.droid.dx.util.ByteArray ba = new com.droid.dx.util.ByteArray(bytes);

        /*
         * First, parse the file completely, so we can safely refer to
         * attributes, etc.
         */
        classFile = new com.droid.dx.cf.direct.DirectClassFile(ba, getFilePath(), getStrictParse());
        classFile.setAttributeFactory(com.droid.dx.cf.direct.StdAttributeFactory.THE_ONE);
        classFile.getMagic(); // Force parsing to happen.

        // Next, reparse it and observe the process.
        com.droid.dx.cf.direct.DirectClassFile liveCf =
            new com.droid.dx.cf.direct.DirectClassFile(ba, getFilePath(), getStrictParse());
        liveCf.setAttributeFactory(com.droid.dx.cf.direct.StdAttributeFactory.THE_ONE);
        liveCf.setObserver(this);
        liveCf.getMagic(); // Force parsing to happen.
    }

    /** {@inheritDoc} */
    @Override
    public void changeIndent(int indentDelta) {
        if (!suppressDump) {
            super.changeIndent(indentDelta);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parsed(com.droid.dx.util.ByteArray bytes, int offset, int len, String human) {
        if (!suppressDump) {
            super.parsed(bytes, offset, len, human);
        }
    }

    /**
     * @param name method name
     * @return true if this method should be dumped
     */
    protected boolean shouldDumpMethod(String name) {
        return args.method == null || args.method.equals(name);
    }

    /** {@inheritDoc} */
    @Override
    public void startParsingMember(com.droid.dx.util.ByteArray bytes, int offset, String name,
                                   String descriptor) {
        if (descriptor.indexOf('(') < 0) {
            // It's a field, not a method
            return;
        }

        if (!shouldDumpMethod(name)) {
            return;
        }

        // Reset the dump cursor to the start of the method.
        setAt(bytes, offset);

        suppressDump = false;

        if (first) {
            first = false;
        } else {
            parsed(bytes, offset, 0, "\n");
        }

        parsed(bytes, offset, 0, "method " + name + " " + descriptor);
        suppressDump = true;
    }

    /** {@inheritDoc} */
    @Override
    public void endParsingMember(com.droid.dx.util.ByteArray bytes, int offset, String name,
                                 String descriptor, com.droid.dx.cf.iface.Member member) {
        if (!(member instanceof Method)) {
            return;
        }

        if (!shouldDumpMethod(name)) {
            return;
        }

        if ((member.getAccessFlags() & (com.droid.dx.rop.code.AccessFlags.ACC_ABSTRACT |
                com.droid.dx.rop.code.AccessFlags.ACC_NATIVE)) != 0) {
            return;
        }

        com.droid.dx.cf.code.ConcreteMethod meth =
            new com.droid.dx.cf.code.ConcreteMethod((Method) member, classFile, true, true);

        if (rop) {
            ropDump(meth);
        } else {
            regularDump(meth);
        }
    }

    /**
     * Does a regular basic block dump.
     *
     * @param meth {@code non-null;} method data to dump
     */
    private void regularDump(com.droid.dx.cf.code.ConcreteMethod meth) {
        BytecodeArray code = meth.getCode();
        com.droid.dx.util.ByteArray bytes = code.getBytes();
        com.droid.dx.cf.code.ByteBlockList list = BasicBlocker.identifyBlocks(meth);
        int sz = list.size();
        com.droid.dx.cf.direct.CodeObserver codeObserver = new com.droid.dx.cf.direct.CodeObserver(bytes, BlockDumper.this);

        // Reset the dump cursor to the start of the bytecode.
        setAt(bytes, 0);

        suppressDump = false;

        int byteAt = 0;
        for (int i = 0; i < sz; i++) {
            com.droid.dx.cf.code.ByteBlock bb = list.get(i);
            int start = bb.getStart();
            int end = bb.getEnd();

            if (byteAt < start) {
                parsed(bytes, byteAt, start - byteAt,
                       "dead code " + Hex.u2(byteAt) + ".." + Hex.u2(start));
            }

            parsed(bytes, start, 0,
                    "block " + Hex.u2(bb.getLabel()) + ": " +
                    Hex.u2(start) + ".." + Hex.u2(end));
            changeIndent(1);

            int len;
            for (int j = start; j < end; j += len) {
                len = code.parseInstruction(j, codeObserver);
                codeObserver.setPreviousOffset(j);
            }

            IntList successors = bb.getSuccessors();
            int ssz = successors.size();
            if (ssz == 0) {
                parsed(bytes, end, 0, "returns");
            } else {
                for (int j = 0; j < ssz; j++) {
                    int succ = successors.get(j);
                    parsed(bytes, end, 0, "next " + Hex.u2(succ));
                }
            }

            ByteCatchList catches = bb.getCatches();
            int csz = catches.size();
            for (int j = 0; j < csz; j++) {
                ByteCatchList.Item one = catches.get(j);
                com.droid.dx.rop.cst.CstType exceptionClass = one.getExceptionClass();
                parsed(bytes, end, 0,
                       "catch " +
                       ((exceptionClass == com.droid.dx.rop.cst.CstType.OBJECT) ? "<any>" :
                        exceptionClass.toHuman()) + " -> " +
                       Hex.u2(one.getHandlerPc()));
            }

            changeIndent(-1);
            byteAt = end;
        }

        int end = bytes.size();
        if (byteAt < end) {
            parsed(bytes, byteAt, end - byteAt,
                    "dead code " + Hex.u2(byteAt) + ".." + Hex.u2(end));
        }

        suppressDump = true;
    }

    /**
     * Does a registerizing dump.
     *
     * @param meth {@code non-null;} method data to dump
     */
    private void ropDump(com.droid.dx.cf.code.ConcreteMethod meth) {
        com.droid.dx.rop.code.TranslationAdvice advice = DexTranslationAdvice.THE_ONE;
        BytecodeArray code = meth.getCode();
        com.droid.dx.util.ByteArray bytes = code.getBytes();
        com.droid.dx.rop.code.RopMethod rmeth = Ropper.convert(meth, advice, classFile.getMethods());
        StringBuffer sb = new StringBuffer(2000);

        if (optimize) {
            boolean isStatic = com.droid.dx.rop.code.AccessFlags.isStatic(meth.getAccessFlags());
            int paramWidth = computeParamWidth(meth, isStatic);
            rmeth =
                com.droid.dx.ssa.Optimizer.optimize(rmeth, paramWidth, isStatic, true, advice);
        }

        com.droid.dx.rop.code.BasicBlockList blocks = rmeth.getBlocks();
        int[] order = blocks.getLabelsInOrder();

        sb.append("first " + Hex.u2(rmeth.getFirstLabel()) + "\n");

        for (int label : order) {
            BasicBlock bb = blocks.get(blocks.indexOfLabel(label));
            sb.append("block ");
            sb.append(Hex.u2(label));
            sb.append("\n");

            IntList preds = rmeth.labelToPredecessors(label);
            int psz = preds.size();
            for (int i = 0; i < psz; i++) {
                sb.append("  pred ");
                sb.append(Hex.u2(preds.get(i)));
                sb.append("\n");
            }

            com.droid.dx.rop.code.InsnList il = bb.getInsns();
            int ilsz = il.size();
            for (int i = 0; i < ilsz; i++) {
                com.droid.dx.rop.code.Insn one = il.get(i);
                sb.append("  ");
                sb.append(il.get(i).toHuman());
                sb.append("\n");
            }

            IntList successors = bb.getSuccessors();
            int ssz = successors.size();
            if (ssz == 0) {
                sb.append("  returns\n");
            } else {
                int primary = bb.getPrimarySuccessor();
                for (int i = 0; i < ssz; i++) {
                    int succ = successors.get(i);
                    sb.append("  next ");
                    sb.append(Hex.u2(succ));

                    if ((ssz != 1) && (succ == primary)) {
                        sb.append(" *");
                    }

                    sb.append("\n");
                }
            }
        }

        suppressDump = false;
        setAt(bytes, 0);
        parsed(bytes, 0, bytes.size(), sb.toString());
        suppressDump = true;
    }
}
