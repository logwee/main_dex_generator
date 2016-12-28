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

package com.droid.dx.rop.code;

/**
 * Plain instruction, which has no embedded data and which cannot possibly
 * throw an exception.
 */
public final class PlainInsn
        extends Insn {
    /**
     * Constructs an instance.
     *
     * @param opcode {@code non-null;} the opcode
     * @param position {@code non-null;} source position
     * @param result {@code null-ok;} spec for the result, if any
     * @param sources {@code non-null;} specs for all the sources
     */
    public PlainInsn(com.droid.dx.rop.code.Rop opcode, SourcePosition position,
                     RegisterSpec result, RegisterSpecList sources) {
        super(opcode, position, result, sources);

        switch (opcode.getBranchingness()) {
            case com.droid.dx.rop.code.Rop.BRANCH_SWITCH:
            case com.droid.dx.rop.code.Rop.BRANCH_THROW: {
                throw new IllegalArgumentException("bogus branchingness");
            }
        }

        if (result != null && opcode.getBranchingness() != com.droid.dx.rop.code.Rop.BRANCH_NONE) {
            // move-result-pseudo is required here
            throw new IllegalArgumentException
                    ("can't mix branchingness with result");
        }
    }

    /**
     * Constructs a single-source instance.
     *
     * @param opcode {@code non-null;} the opcode
     * @param position {@code non-null;} source position
     * @param result {@code null-ok;} spec for the result, if any
     * @param source {@code non-null;} spec for the source
     */
    public PlainInsn(com.droid.dx.rop.code.Rop opcode, SourcePosition position, RegisterSpec result,
                     RegisterSpec source) {
        this(opcode, position, result, RegisterSpecList.make(source));
    }

    /** {@inheritDoc} */
    @Override
    public com.droid.dx.rop.type.TypeList getCatches() {
        return com.droid.dx.rop.type.StdTypeList.EMPTY;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(Visitor visitor) {
        visitor.visitPlainInsn(this);
    }

    /** {@inheritDoc} */
    @Override
    public Insn withAddedCatch(com.droid.dx.rop.type.Type type) {
        throw new UnsupportedOperationException("unsupported");
    }

    /** {@inheritDoc} */
    @Override
    public Insn withRegisterOffset(int delta) {
        return new PlainInsn(getOpcode(), getPosition(),
                             getResult().withOffset(delta),
                             getSources().withOffset(delta));
    }

    /** {@inheritDoc} */
    @Override
    public Insn withSourceLiteral() {
        RegisterSpecList sources = getSources();
        int szSources = sources.size();

        if (szSources == 0) {
            return this;
        }

        com.droid.dx.rop.type.TypeBearer lastType = sources.get(szSources - 1).getTypeBearer();

        if (!lastType.isConstant()) {
            // Check for reverse subtraction, where first source is constant
            com.droid.dx.rop.type.TypeBearer firstType = sources.get(0).getTypeBearer();
            if (szSources == 2 && firstType.isConstant()) {
                com.droid.dx.rop.cst.Constant cst = (com.droid.dx.rop.cst.Constant) firstType;
                RegisterSpecList newSources = sources.withoutFirst();
                com.droid.dx.rop.code.Rop newRop = Rops.ropFor(getOpcode().getOpcode(), getResult(),
                                             newSources, cst);
                return new PlainCstInsn(newRop, getPosition(), getResult(),
                                            newSources, cst);
            }
            return this;
        } else {

            com.droid.dx.rop.cst.Constant cst = (com.droid.dx.rop.cst.Constant) lastType;

            RegisterSpecList newSources = sources.withoutLast();

            com.droid.dx.rop.code.Rop newRop;
            try {
                // Check for constant subtraction and flip it to be addition
                int opcode = getOpcode().getOpcode();
                if (opcode == RegOps.SUB && cst instanceof com.droid.dx.rop.cst.CstInteger) {
                    opcode = RegOps.ADD;
                    cst = com.droid.dx.rop.cst.CstInteger.make(-((com.droid.dx.rop.cst.CstInteger)cst).getValue());
                }
                newRop = Rops.ropFor(opcode, getResult(), newSources, cst);
            } catch (IllegalArgumentException ex) {
                // There's no rop for this case
                return this;
            }

            return new PlainCstInsn(newRop, getPosition(),
                    getResult(), newSources, cst);
        }
    }


    /** {@inheritDoc} */
    @Override
    public Insn withNewRegisters(RegisterSpec result,
            RegisterSpecList sources) {

        return new PlainInsn(getOpcode(), getPosition(),
                             result,
                             sources);

    }
}
