## environment

https://code.educoder.net/ppg69fuwb/riscv-lab

此处直接看项目的 README 即可。

## Lab1 - Code

每一个地方的修改有对应 LABx 的注释，可以利用 TODO Tree 快速定位查看。

`playground/src/defines/isa/Instructions.scala`：

```scala
  // LAB1: ALUOpType
  def add  = 0.U
  def sub  = 1.U
  def sll  = 2.U
  def slt  = 3.U
  def sltu = 4.U
  def xor  = 5.U
  def srl  = 6.U
  def sra  = 7.U
  def or   = 8.U
  def and  = 9.U
  def addw = 10.U
  def subw = 11.U
  def sllw = 12.U
  def srlw = 13.U
  def sraw = 14.U
```

先直接顺序编号，后续可以再修改，其它模块只会使用 `ALUOpType.add` 形式的别名而不会使用具体的数字，不会出现连带修改的问题。

`playground/src/defines/isa/RVI.scala`：

```scala
    // LAB1: RV32I_ALUInstr : R type
    ADD  -> List(InstrR, FuType.alu, ALUOpType.add),
    SLL  -> List(InstrR, FuType.alu, ALUOpType.sll),
    SLT  -> List(InstrR, FuType.alu, ALUOpType.slt),
    SLTU -> List(InstrR, FuType.alu, ALUOpType.sltu),
    XOR  -> List(InstrR, FuType.alu, ALUOpType.xor),
    SRL  -> List(InstrR, FuType.alu, ALUOpType.srl),
    OR   -> List(InstrR, FuType.alu, ALUOpType.or),
    AND  -> List(InstrR, FuType.alu, ALUOpType.and),
    SUB  -> List(InstrR, FuType.alu, ALUOpType.sub),
    SRA  -> List(InstrR, FuType.alu, ALUOpType.sra),

    // LAB1: RV64IInstr : R type
    SLLW -> List(InstrR, FuType.alu, ALUOpType.sllw),
    SRLW -> List(InstrR, FuType.alu, ALUOpType.srlw),
    SRAW -> List(InstrR, FuType.alu, ALUOpType.sraw),
    ADDW -> List(InstrR, FuType.alu, ALUOpType.addw),
    SUBW -> List(InstrR, FuType.alu, ALUOpType.subw),
```

这个地方实际上最后会将所有指令列表连接成一张总表：

```scala
object RVIInstr extends CoreParameter {
  val table = RV32I_ALUInstr.table ++
    (if (XLEN == 64) RV64IInstr.table else Array.empty)
}
```

之后在 `decode` 阶段，我们需要用到这张总表来做模式匹配分析出指令类型、指令使用的 `FU` 部件和指令使用的运算编号，那时候这里的 `RVIInstr` 会被重载为 `Instructions.DecodeTable`。

`playground/src/pipeline/decode/ARegfile.scala`：

```scala
  // LAB1: Initialize Register regs(i) = i
  val regs = RegInit(VecInit(
     0.U(XLEN.W),  1.U(XLEN.W),  2.U(XLEN.W),  3.U(XLEN.W),
     4.U(XLEN.W),  5.U(XLEN.W),  6.U(XLEN.W),  7.U(XLEN.W),
     8.U(XLEN.W),  9.U(XLEN.W), 10.U(XLEN.W), 11.U(XLEN.W),
    12.U(XLEN.W), 13.U(XLEN.W), 14.U(XLEN.W), 15.U(XLEN.W),
    16.U(XLEN.W), 17.U(XLEN.W), 18.U(XLEN.W), 19.U(XLEN.W),
    20.U(XLEN.W), 21.U(XLEN.W), 22.U(XLEN.W), 23.U(XLEN.W),
    24.U(XLEN.W), 25.U(XLEN.W), 26.U(XLEN.W), 27.U(XLEN.W),
    28.U(XLEN.W), 29.U(XLEN.W), 30.U(XLEN.W), 31.U(XLEN.W),
  ))

  // LAB1: Register : Write
  when (io.write.wen && (io.write.waddr =/= 0.U)) {
    regs(io.write.waddr) := io.write.wdata
  }

  // LAB1: Register : Read
  io.read.src1.rdata := regs(io.read.src1.raddr)
  io.read.src2.rdata := regs(io.read.src2.raddr)
```

这里 lab1 需要寄存器初始化 `regs(i) = i`，但是写丑了，直接手动了。lab2 开始这个部分要改回 `regs(i) = 0`。

写寄存器堆部分一开始忘记判地址是不是 `0` 了，`regs(0) = 0` 是 isa 标准规定，但没有怎么影响实验进度，啥时候想起来就看了一眼改了一下。

`playground/src/pipeline/decode/Decoder.scala`：

```scala
  // LAB1: Decoder
  io.out.info.valid      := instrType =/= InstrN
  io.out.info.src1_raddr := rs
  io.out.info.src2_raddr := rt
  io.out.info.op         := fuOpType
  io.out.info.reg_wen    := (instrType =/= InstrS) && (instrType =/= InstrB)
  io.out.info.reg_waddr  := rd
```

这个部分本来是要做模式匹配的。就是找到 `playground/src/defines/isa/RVI.scala` 中哪个可以 `BitPat` 上，需要写一些循环判断啥的。但是后面助教补充了 `ListLookup`，这就不需要我写了！本来我就要写神秘 `array` 转 `map` 了。

这六个项依次代表：

1. `valid`：是否有效
2. `src1_raddr`：第一个操作数的寄存器地址
3. `src2_raddr`：第二个操作数的寄存器地址
4. `op`：使用的运算编号
5. `reg_wen`：结果是否要写寄存器
6. `reg_waddr`：结果写寄存器的地址

`valid` 就只需要考虑能不能 `BitPat` 了，`ListLookup` 中有特殊的找不到的默认值 `InstrN` 所以很容易判断。

观察过了，只有 `S, B` 型指令不需要写回寄存器，所以就这么写 `reg_wen`。

`playground/src/pipeline/decode/DecodeUnit.scala`：

```scala
  // LAB1: DecodeUnit : Decode -> addr -> Register
  io.regfile.src1.raddr := info.src1_raddr
  io.regfile.src2.raddr := info.src2_raddr

  // LAB1: DecodeUnit : Register -> data -> Execute
  io.executeStage.data.src_info.src1_data := io.regfile.src1.rdata
  io.executeStage.data.src_info.src2_data := io.regfile.src2.rdata
```

这里就是连接一下寄存器堆对指令进行进一步解析，实际上就是从寄存器堆中读出两个操作数传给 `execute` 模块。

`playground/src/pipeline/execute/ExecuteStage.scala`：

```scala
  // LAB1: ALU
  val valid = io.info.valid
  val op    = io.info.op
  val rs    = io.src_info.src1_data
  val rt    = io.src_info.src2_data

  def W(x : UInt) = {
    val x32 = x(31, 0)
    Cat(Fill(32, x32(31)), x32)
  }

  val res = Wire(UInt(XLEN.W))

  res := 0.U

  when (valid) {
    switch (op) {
      is (ALUOpType. add) { res := rs + rt }
      is (ALUOpType. sub) { res := rs - rt }
      is (ALUOpType. sll) { res := rs << rt(5, 0) }
      is (ALUOpType. slt) { res := (rs.asSInt < rt.asSInt).asUInt }
      is (ALUOpType.sltu) { res := (rs < rt).asUInt }
      is (ALUOpType. xor) { res := rs ^ rt }
      is (ALUOpType. srl) { res := rs >> rt(5, 0) }
      is (ALUOpType. sra) { res := (rs.asSInt >> rt(5, 0)).asUInt }
      is (ALUOpType.  or) { res := rs | rt }
      is (ALUOpType. and) { res := rs & rt }
      is (ALUOpType.addw) { res := W(rs + rt) }
      is (ALUOpType.subw) { res := W(rs - rt) }
      is (ALUOpType.sllw) { res := W(rs << rt(5, 0)) }
      is (ALUOpType.srlw) { res := W(rs >> rt(5, 0)) }
      is (ALUOpType.sraw) { res := W((rs.asSInt >> rt(5, 0)).asUInt) }
    }
  }

  io.result := res
```

这个部分对着 RISC-V 手册写，不要瞎搞就行。

为了更好的处理 RV64 中带 `W` 后缀的指令（也就是 $32$ 位运算指令），我们实现了一个函数 `W` 来实现 $32$ 位到 $64$ 位的有符号扩展操作，这使得 `ALU` 的实现更加简洁易懂，同时提高了代码复用性。

`playground/src/pipeline/execute/ExecuteUnit.scala`：

```scala
  // LAB1: ExecuteUnit
  io.memoryStage.data.pc       := fu.data.pc
  io.memoryStage.data.info     := fu.data.info
  io.memoryStage.data.src_info := fu.data.src_info
  io.memoryStage.data.rd_info  := fu.data.rd_info
```

把该传的传一下就行。

这里了解了一下 `=` 和 `:=` 的区别，在 chisel 中 `=` 是用于变量声明和初始化，`:=` 用于硬件连接赋值。

`playground/src/pipeline/memory/MemoryStage.scala`：

```scala
  // LAB1: MemoryStage
  data := io.executeUnit.data
  io.memoryUnit.data := data
```

各个 `Stage` 的作用就是暂存后传一下 `data`，很容易的。

注意这里的 `data` 是寄存器类型，这样才能实现时钟控制流水线。

`playground/src/pipeline/writeback/WriteBackStage.scala`：

```scala
  // LAB1: WriteBackStage
  data := io.memoryUnit.data
  io.writeBackUnit.data := data
```

`playground/src/pipeline/writeback/WriteBackUnit.scala`：

```scala
  // LAB1: WriteBack -> data -> Register
  val info    = io.writeBackStage.data.info
  val rd_info = io.writeBackStage.data.rd_info

  io.regfile.wen   := info.valid && info.reg_wen
  io.regfile.waddr := info.reg_waddr
  io.regfile.wdata := rd_info.wdata

  // LAB1: Difftest : Commit Debug
  io.debug.commit   := info.valid
  io.debug.pc       := io.writeBackStage.data.pc
  io.debug.rf_wnum  := info.reg_waddr
  io.debug.rf_wdata := rd_info.wdata
```

这里要注意把 `debug` 信息传出去。

`playground/src/Core.scala`：

```scala
  // LAB1: Decode
  decodeStage.decodeUnit <> decodeUnit.decodeStage
  decodeUnit.regfile <> regfile.read
  decodeUnit.executeStage <> executeStage.decodeUnit

  // LAB1: Execute
  executeStage.executeUnit <> executeUnit.executeStage
  executeUnit.dataSram <> io.dataSram
  executeUnit.memoryStage <> memoryStage.executeUnit

  // LAB1: Memory
  memoryStage.memoryUnit <> memoryUnit.memoryStage
  memoryUnit.writeBackStage <> writeBackStage.memoryUnit

  // LAB1: Writeback
  writeBackStage.writeBackUnit <> writeBackUnit.writeBackStage
  writeBackUnit.regfile <> regfile.write

  // LAB1: Difftest
  writeBackUnit.debug <> io.debug
```

根据整个流水线的图来看怎么连接总线就很容易了。

注意这里需要特别处理寄存器堆 `regfile` 和数据存储器 `dataSram` 的一些事情，以及把 `debug` 丢出去的事情。

## Lab1 - report

鸽。

## Lab1 - Thinking & Exploration

1. RISC-V 指令集是定长指令集吗？

   RISC-V 的基础指令集 RV32 和 RV64 均是 $32$ 位定长指令集，但存在诸如 RVC $16$ 位压缩指令集的扩展。

2. RV64 和 RV32 的 `R` 型运算指令是否有区别？

   有区别。主要区别在于 RV64 中新增助记符带 `W` 的 `R` 型运算字指令，是 RV64 中专门用于处理 $32$ 位数据的指令，意为截取最后 $32$ 位运算后有符号扩展为 $64$ 位。

3. 设计比较指令 `slt` 和 `sltu` 的目的是什么？

   在补码存储方式下，有符号整数和无符号整数的比较方式是不一致的。有符号时最高位为符号位，$0$ 大 $1$ 小；而无符号时最高位为数值位 $1$ 大 $0$ 小。

   一般而言，这两种比较在硬件实现上很不一样，在指令集中区分它们可以更好的精简硬件实现。

4. `sll, srl, sra` 这三条指令在 `rs2[63:6]` 不全为 $0$ 时，指令的执行结果是什么？

   按照 RISC-V 标准，高位会直接被忽略，与 `rs2[63:6]` 全为 $0$ 时的指令执行结果相同。

5. RISC-V 的运算指令有进行运算结果的溢出判断吗？为什么？

   RISC-V 的基础指令集 RV32 和 RV64 均不主动进行运算结果的溢出判断，这是由于其简化硬件和增强灵活性的设计哲学决定的。

6. 为什么并不是所有的 `R` 型运算指令都有对应的字指令（助记符带 `W` 的指令）？

   实际上所有 `W` 指令都可以通过一些非 `W` 指令的组合实现，`W` 指令本身是为了从硬件层面上在 RV64 中优化 $32$ 位数据的运算。不同的 `R` 型 `W` 指令的组合难度不同，困难的就从硬件层面实现，简单的就不实现。

   具体而言分为 $4$ 类指令：

   1. `add, sub` 等数值运算指令：有符号情况下 $32$ 位数据的符号位在第 $31$ 位，会涉及到符号扩展的问题，需要特殊实现 `W` 指令。
   2. `slt, sltu` 等比较运算指令：通过一些截断和反转位操作，$32$ 位和 $64$ 位比较实际上没有区别，不需要特殊实现 `W` 指令。
   3. `and, xor` 等位运算指令：只要保证高 $32$ 位均为 $0$，$32$ 位和 $64$ 位比较实际上没有区别，不需要特殊实现 `W` 指令。
   4. `sll, sra` 等移位指令：$32$ 位数据需要限制 `rs2` 只有低 `5` 位有效，同时也存在符号扩展问题，需要特殊实现 `W` 指令。

7. 请问差分测试框架只用图 9-11 中的 $4$ 个 `debug` 信号够吗？假如有的指令不将结果写回通用寄存器，这时框架该如何发现问题？

   个人认为不太够。`R` 型指令都在通用寄存器操作这样做问题不大，但在后续出现能够操作内存的指令时，这样的 $4$ 个 `debug` 信号并不足够，如果后续 lab 中不能改动的话可能需要细致的设计测试用例等手段来实现更为准确的差分测试。

8. 当前处理器采用的是哈佛结构还是冯诺依曼结构？

   哈佛结构。lab1 文档中明确指出指令存储器`instMEM` 和数据存储器 `dataMEM` 存放在两个完全独立的 SRAM 模块中，数据存储和指令存储分开是典型的哈佛结构。

## Lab2 - Code

`playground/src/defines/Bundles.scala`：

```scala
  // LAB2: New Info
  val src1_ren   = Bool()
  val src2_ren   = Bool()
  val imm        = UInt(XLEN.W)
  val src1_pcen  = Bool()
```

给 `Info` 中增加一些可以传递的信息，四个项分别是：

1. `src1_ren`：第一个操作数是否要读寄存器
2. `src2_ren`：第二个操作数是否要读寄存器
3. `imm`：立即数
4. `src1_pcen`：第一个操作数是否要读 `pc`

虽然 lab2 文档中推荐把整条指令 `inst` 也打包进去，但我没有这么做。主要是因为我认为这样子会混淆 `Decoder` 和 `DecodeUnit` 要做的事情：`Decoder` 负责解析出所有可以从指令本身解析出的东西，`DecodeUnit` 负责根据 `Decoder` 提供的信息读寄存器和 `pc`。

`playground/src/defines/isa/RVI.scala`：

```scala
    // LAB2: RV32I_ALUInstr : I type
    ADDI  -> List(InstrI, FuType.alu, ALUOpType.add),
    SLLI  -> List(InstrI, FuType.alu, ALUOpType.sll),
    SLTI  -> List(InstrI, FuType.alu, ALUOpType.slt),
    SLTIU -> List(InstrI, FuType.alu, ALUOpType.sltu),
    XORI  -> List(InstrI, FuType.alu, ALUOpType.xor),
    SRLI  -> List(InstrI, FuType.alu, ALUOpType.srl),
    ORI   -> List(InstrI, FuType.alu, ALUOpType.or),
    ANDI  -> List(InstrI, FuType.alu, ALUOpType.and),
    SRAI  -> List(InstrI, FuType.alu, ALUOpType.sra),

    // LAB2: RV32I_ALUInstr : U type
    AUIPC -> List(InstrU, FuType.alu, ALUOpType.add),
    LUI   -> List(InstrU, FuType.alu, ALUOpType.add),

    // LAB2: RV64IInstr : I type
    SLLIW -> List(InstrI, FuType.alu, ALUOpType.sllw),
    SRLIW -> List(InstrI, FuType.alu, ALUOpType.srlw),
    SRAIW -> List(InstrI, FuType.alu, ALUOpType.sraw),
    ADDIW -> List(InstrI, FuType.alu, ALUOpType.addw),
```

增加了一些 `I` 型和 `U` 型指令，没什么好说的。

`playground/src/pipeline/decode/ARegfile.scala`：

```scala
  // LAB2: Initialize Register regs(i) = 0
  val regs = RegInit(VecInit(Seq.fill(AREG_NUM)(0.U(XLEN.W))))
```

lab2 开始这个部分改回 `regs(i) = 0`。

`playground/src/pipeline/decode/Decoder.scala`：

```scala
  // LAB2: Decoder
  io.out.info.src1_ren   := (instrType =/= InstrU) && (instrType =/= InstrJ)
  io.out.info.src2_ren   := (instrType =/= InstrI) && (instrType =/= InstrU) && (instrType =/= InstrJ)
  io.out.info.src1_pcen  := inst === BitPat("b????????????????????_?????_0010111")
  
  // LAB2: Decoder : imm
  val imm = Wire(UInt(XLEN.W))
  imm := 0.U
  switch (instrType) {
    is (InstrI) {
      val imm12 = inst(31, 20)
      imm := Cat(Fill(54, imm12(11)), imm12)
    }
    is (InstrU) {
      val imm32 = inst(31, 12) << 12
      imm := Cat(Fill(32, imm32(31)), imm32)
    }
  }
  io.out.info.imm := imm
```

`src1_ren` 和 `src2_ren` 对着 RISC-V 手册很容易写出来。

`src1_pcen` 暂且只知道有 `auipc` 指令需要读 `pc`，后续可能要修改。

`imm` 目前对着 RISC-V 手册只解析了 `I` 型和 `U` 型，后续用到了其它类型再加。

`playground/src/pipeline/decode/Decoder.scala`：

```scala
  // LAB2: DecodeUnit : Register / imm / pc -> data -> Execute
  val src1_table = IndexedSeq(
    info.src1_ren  -> io.regfile.src1.rdata,
    info.src1_pcen -> pc,
  )
  io.executeStage.data.src_info.src1_data := MuxCase(0.U, src1_table)
  
  val src2_table = IndexedSeq(
    info.src2_ren  -> io.regfile.src2.rdata,
  )
  io.executeStage.data.src_info.src2_data := MuxCase(info.imm, src2_table)
```

这里使用多路选择器处理选择读寄存器、`pc`、立即数还是 `0`，估计后面还得接着加。

`playground/src/pipeline/execute/fu/Alu.scala`：

```scala
      // LAB2: LAB1 Wrong
      // sllw & srlw & sraw : rt(5, 0) -> rt(4, 0)
      // srlw & sraw        : rs -> rs(31, 0)
      is (ALUOpType.sllw) { res := W(rs << rt(4, 0)) }
      is (ALUOpType.srlw) { res := W(rs(31, 0) >> rt(4, 0)) }
      is (ALUOpType.sraw) { res := W((rs(31, 0).asSInt >> rt(4, 0)).asUInt) }
```

这里在 lab1 的时候实现有误，但因为没有立即数相关的指令这些问题很难卡出来所以被拖到了 lab2 才被卡出来。

具体其实就是没有完全对着 RISC-V 手册写导致的，主要是 `W` 指令位移 `rs2` 要取低 $5$ 位而不是 $6$ 位，以及 `rs1` 要直接先截断成 $32$ 位。

调试的时候在汇编文件 `lab2.asm` 中读到差分测试出错的指令是这几条位运算，第一反应就是直接重新读一遍 RISC-V 手册，大概率是实现和手册不一致导致的这类问题，所以也没有用上 gtkwave 啥的调试工具。

这里还有一个问题，在老版的 RISC-V 手册中 `srai` 等指令要求 `imm` 的第 $5$ 位必须是 $0$ 才能有效，然而查阅最新的手册发现，现在这个问题已经变成一个可选的情况了。事实上这个问题在模式匹配 `BitPat` 中是按照老版写了的。

## Lab2 - report

鸽。

## Lab2 - Thinking & Exploration

1. 为什么在 RISC-V 指令中没有 `subi` 指令？

   可以将 `imm` 直接取负，从而将 `subi` 转化为 `addi`。

   这个事情完全可以在编译器层级直接解决，不需要额外增加硬件负担。

2. 观察表 9-7 中指令的 `imm` 字段，为什么 `imm` 字段的长度被设计为 `20` 位？请问这样设计可以和哪些指令搭配使用并发挥什么样的效果？

   `U` 型指令的 $20$ 位高位立即数（`31:12` 位）和 `I` 型指令的 $12$ 位低位立即数（`11:0` 位）可以配合起来实现 `32` 位立即数的功能。

   具体而言，如果想实现对 `x1` 寄存器加 $32$ 位立即数 `imm`，那么 RISC-V 汇编指令中应该这么写：

   ```assembly
   lui x2,imm[31:12]
   addi x1,x2,imm[11:0]
   ```

   其中 `x2` 是一个暂存用的寄存器。

## git & github 小插曲

原来的远程仓库地址：

```bash
https://bdgit.educoder.net/ppg69fuwb/riscv-lab.git
```

改成自己的 ssh key 就可以丢到 github 上了！具体用如下指令修改远程仓库地址：

```
git remote set-url origin git@github.com:hezlik/hdu-cpu-lab.git
```

然后 `git push` 就可以丢自己仓库了！

需要实验框架的更新可以再改回来 `git pull`。

## Lab3 - Code

`playground/src/defines/isa/Instructions.scala`：

```scala
// LAB3: FuType
object FuType {
  def num        = 2
  def alu        = 0.U
  def mdu        = 1.U
  def apply() = UInt(log2Up(num).W)
}

// LAB3 : MDUOpType
object MDUOpType {

  def mul    = 0.U
  def mulh   = 1.U
  def mulhsu = 2.U
  def mulhu  = 3.U
  def div    = 4.U
  def divu   = 5.U
  def rem    = 6.U
  def remu   = 7.U
  def mulw   = 8.U
  def divw   = 9.U
  def divuw  = 10.U
  def remw   = 12.U
  def remuw  = 13.U

}
```

这里新增一种 `FU` 部件即 `MDU`，专门用来处理乘除法运算指令，即 RV32/64M 指令集。

`playground/src/defines/isa/RVI.scala`：

```scala
// LAB3: RV32MInstr
object RV32MInstr extends HasInstrType with CoreParameter {
  
  def MUL    = BitPat("b0000001_?????_?????_000_?????_0110011")
  def MULH   = BitPat("b0000001_?????_?????_001_?????_0110011")
  def MULHSU = BitPat("b0000001_?????_?????_010_?????_0110011")
  def MULHU  = BitPat("b0000001_?????_?????_011_?????_0110011")
  
  def DIV    = BitPat("b0000001_?????_?????_100_?????_0110011")
  def DIVU   = BitPat("b0000001_?????_?????_101_?????_0110011")
  def REM    = BitPat("b0000001_?????_?????_110_?????_0110011")
  def REMU   = BitPat("b0000001_?????_?????_111_?????_0110011")

  val table = Array(

    MUL    -> List(InstrR, FuType.mdu, MDUOpType.mul),
    MULH   -> List(InstrR, FuType.mdu, MDUOpType.mulh),
    MULHSU -> List(InstrR, FuType.mdu, MDUOpType.mulhsu),
    MULHU  -> List(InstrR, FuType.mdu, MDUOpType.mulhu),
    
    DIV    -> List(InstrR, FuType.mdu, MDUOpType.div),
    DIVU   -> List(InstrR, FuType.mdu, MDUOpType.divu),
    REM    -> List(InstrR, FuType.mdu, MDUOpType.rem),
    REMU   -> List(InstrR, FuType.mdu, MDUOpType.remu),

  )

}

// LAB3: RV64MInstr
object RV64MInstr extends HasInstrType with CoreParameter {

  def MULW  = BitPat("b0000001_?????_?????_000_?????_0111011")
  
  def DIVW  = BitPat("b0000001_?????_?????_100_?????_0111011")
  def DIVUW = BitPat("b0000001_?????_?????_101_?????_0111011")
  def REMW  = BitPat("b0000001_?????_?????_110_?????_0111011")
  def REMUW = BitPat("b0000001_?????_?????_111_?????_0111011")

  val table = Array(

    MULW  -> List(InstrR, FuType.mdu, MDUOpType.mulw),
    
    DIVW  -> List(InstrR, FuType.mdu, MDUOpType.divw),
    DIVUW -> List(InstrR, FuType.mdu, MDUOpType.divuw),
    REMW  -> List(InstrR, FuType.mdu, MDUOpType.remw),
    REMUW -> List(InstrR, FuType.mdu, MDUOpType.remuw),

  )
  
}

object RVIInstr extends CoreParameter {
  val table = RV32I_ALUInstr.table ++
    (if (XLEN == 64) RV64IInstr.table else Array.empty) ++
    // LAB3: RVIInstr : RV32MInstr & RV64MInstr
    RV32MInstr.table ++
    (if (XLEN == 64) RV64MInstr.table else Array.empty)
}
```

模仿之前的 `RV32I_ALUInstr` 和 `RV64IInstr` 写就可以了，注意检查一下不要抄错 RISC-V 手册。

这里这样搞其实命名就不太对，应该把 `RVIInstr` 改成 `RVInstr`，不过这个地方改了还得改后面的 `Decoder.scala`，有点麻烦且无伤大雅就没改。

`playground/src/defines/Bundles.scala`：

```scala
  // LAB3: New Info
  val fusel      = FuType()
```

就新加一个 `fusel` 表示使用哪个 `FU` 部件就行了。

`playground/src/pipeline/decode/Decoder.scala`：

```scala
  // LAB3: Decoder
  io.out.info.fusel      := fuType
```

`Decoder.scala` 中也需要提取出使用哪个 `FU` 部件的信息。

`playground/src/pipeline/execute/Fu.scala`：

```scala
	// LAB3: Reconstruct Logic of FU
  val res = Wire(UInt(XLEN.W))

  res := 0.U
  
  switch (io.data.info.fusel) {
    is (FuType.alu) {
      val alu = Module(new Alu()).io
      alu.info     := io.data.info
      alu.src_info := io.data.src_info
      res          := alu.result
    }
    is (FuType.mdu) {
      val mdu = Module(new Mdu()).io
      mdu.info     := io.data.info
      mdu.src_info := io.data.src_info
      res          := mdu.result
    }
  }

  io.data.rd_info.wdata := res
```

此时需要在 `Fu.scala` 中实现按照 `fusel` 选择对应 `FU` 部件的功能。

这里注意到一个事情，我们没有在这里处理 `io.data.info.valid` 的事情，事实上在电路中只要有任意一层处理 `valid` 就行了，因为任意一层断路都可以做到让这个电信号传不到 `ExecuteUnit`。这里将 `valid` 的处理直接丢给了底层的 `LU` 分部件（即 `ALU` 之类的）。

`playground/src/pipeline/execute/fu/Mdu.scala`：

```scala
// LAB3: MDU Module

package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class Mdu extends Module {
  val io = IO(new Bundle {
    val info     = Input(new Info())
    val src_info = Input(new SrcInfo())
    val result   = Output(UInt(XLEN.W))
  })

  val valid = io.info.valid
  val op    = io.info.op
  val rs    = io.src_info.src1_data
  val rt    = io.src_info.src2_data

  def W(x : UInt) = {
    val x32 = x(31, 0)
    Cat(Fill(32, x32(31)), x32)
  }

  val res = Wire(UInt(XLEN.W))

  res := 0.U

  when (valid) {
    switch (op) {
      is (MDUOpType.   mul) { res := rs * rt }
      is (MDUOpType.  mulh) { res := (rs.asSInt * rt.asSInt)(127, 64) }
      is (MDUOpType.mulhsu) { res := (rs.asSInt * rt).asSInt(127, 64) }
      is (MDUOpType. mulhu) { res := (rs * rt)(127, 64) }
      is (MDUOpType.   div) {
        res := Mux(
          rt === 0.U,
          "hffffffffffffffff".U,
          (rs.asSInt / rt.asSInt)(63, 0)
        )
      }
      is (MDUOpType.  divu) {
        res := Mux(
          rt === 0.U,
          "hffffffffffffffff".U,
          rs / rt
        )
      }
      is (MDUOpType.   rem) {
        res := Mux(
          rt === 0.U,
          rs,
          (rs.asSInt - rt.asSInt * (rs.asSInt / rt.asSInt))(63, 0)
        )
      }
      is (MDUOpType.  remu) {
        res := Mux(
          rt === 0.U,
          rs,
          rs % rt
        )
      }
      is (MDUOpType.  mulw) { res := W(rs * rt) }
      is (MDUOpType.  divw) {
        res := Mux(
          rt(31,0) === 0.U,
          "hffffffffffffffff".U,
          W((rs(31, 0).asSInt / rt(31, 0).asSInt).asUInt)
        )
      }
      is (MDUOpType. divuw) {
        res := Mux(
          rt(31, 0) === 0.U,
          "hffffffffffffffff".U,
          W(rs(31, 0) / rt(31, 0))
        )
      }
      is (MDUOpType.  remw) {
        res := Mux(
          rt(31, 0) === 0.U,
          W(rs(31, 0)),
          W((rs(31, 0).asSInt % rt(31, 0).asSInt).asUInt)
        )
      }
      is (MDUOpType. remuw) {
        res := Mux(
          rt(31, 0) === 0.U,
          W(rs(31,0)),
          W(rs(31, 0) % rt(31, 0))
        )
      }
    }
  }
  
  io.result := res

}
```

这个具体实现 `MDU` 的部分有很多问题，主要就是不太了解 RISC-V 手册中的乘除法和 `scala` 中的乘除法在各种情况下的结果，最后情况都是试出来的。

首先我先假设 `scala` 中的乘除法和 RISC-V 手册中的乘除法行为一致，写了第一个版本。然后反复差分测试，发现问题主要在于除法：

1. 除法的除数为 $0$ 时，需要让商为整型最大值，余数为被除数。
2. 有符号取余要通过被除数 $-$ 商 $\times$ 除数实现。

我觉得这个部分的测试应该不是很强，大概率写的代码还有问题，但是问题可以先交给以后的我！~~估计乘除法部分后面也不太会有测试。~~

## Lab3 - report

鸽。

## Lab3 - Thinking & Exploration

鸽。

## Lab4 - Code

`playground/src/defines/isa/Instructions.scala`：

```scala
// LAB4: FuType
object FuType {
  def num     = 3
  def alu     = 0.U
  def mdu     = 1.U
  def lsu     = 2.U
  def apply() = UInt(log2Up(num).W)
}

// LAB4: LSUOpType
object LSUOpType {

  def lb  = 0.U
  def lh  = 1.U
  def lw  = 2.U
  def ld  = 3.U
  def lbu = 4.U
  def lhu = 5.U
  def lwu = 6.U
  def sb  = 7.U
  def sh  = 8.U
  def sw  = 9.U
  def sd  = 10.U

}
```

`playground/src/defines/isa/RVI.scala`：

```scala
// LAB4: RV32I_LSUInstr
object RV32I_LSUInstr extends HasInstrType with CoreParameter {
  
  def LB  = BitPat("b????????????_?????_000_?????_0000011")
  def LH  = BitPat("b????????????_?????_001_?????_0000011")
  def LW  = BitPat("b????????????_?????_010_?????_0000011")
  def LD  = BitPat("b????????????_?????_011_?????_0000011")
  def LBU = BitPat("b????????????_?????_100_?????_0000011")
  def LHU = BitPat("b????????????_?????_101_?????_0000011")
  def LWU = BitPat("b????????????_?????_110_?????_0000011")

  def SB  = BitPat("b???????_?????_?????_000_?????_0100011")
  def SH  = BitPat("b???????_?????_?????_001_?????_0100011")
  def SW  = BitPat("b???????_?????_?????_010_?????_0100011")
  def SD  = BitPat("b???????_?????_?????_011_?????_0100011")

  val table = Array(

    LB  -> List(InstrI, FuType.lsu, LSUOpType.lb),
    LH  -> List(InstrI, FuType.lsu, LSUOpType.lh),
    LW  -> List(InstrI, FuType.lsu, LSUOpType.lw),
    LD  -> List(InstrI, FuType.lsu, LSUOpType.ld),
    LBU -> List(InstrI, FuType.lsu, LSUOpType.lbu),
    LHU -> List(InstrI, FuType.lsu, LSUOpType.lhu),
    LWU -> List(InstrI, FuType.lsu, LSUOpType.lwu),

    SB  -> List(InstrS, FuType.lsu, LSUOpType.sb),
    SH  -> List(InstrS, FuType.lsu, LSUOpType.sh),
    SW  -> List(InstrS, FuType.lsu, LSUOpType.sw),
    SD  -> List(InstrS, FuType.lsu, LSUOpType.sd),

  )

}

object RVIInstr extends CoreParameter {
  val table = RV32I_ALUInstr.table ++
    (if (XLEN == 64) RV64IInstr.table else Array.empty) ++
    // LAB3: RVIInstr : RV32MInstr & RV64MInstr
    RV32MInstr.table ++
    (if (XLEN == 64) RV64MInstr.table else Array.empty) ++
    // LAB4: RVIInstr : RV32I_LSUInstr
    RV32I_LSUInstr.table
}
```

`playground/src/pipeline/decode/Decoder.scala`：

```scala
    // LAB4: Decoder : imm : InstrS
    is (InstrS) {
      val imm12 = Cat(inst(31, 25), inst(11, 7))
      imm := Cat(Fill(54, imm12(11)), imm12)
    }
```

`Decoder` 中对立即数 `imm` 的解析增加 `S` 型指令。

`playground/src/pipeline/execute/Fu.scala`：

```scala
    // LAB4: New FU : LSU
    is (FuType.lsu) {
      val lsu = Module(new Lsu()).io
      lsu.info     := io.data.info
      lsu.src_info := io.data.src_info
      lsu.dataSram <> io.dataSram
    }
```

新增部件 `LSU`，注意这里的电路连接与之前的 `ALU` 和 `MDU` 并不完全相同，`LSU` 不返回结果 `result`，但需要接入 `dataSram`。

`playground/src/pipeline/execute/fu/Lsu.scala`：

```scala
// LAB4: LSU Module in Execute for Storage

package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class Lsu extends Module {
  val io = IO(new Bundle {
    val info     = Input(new Info())
    val src_info = Input(new SrcInfo())
    val dataSram = new DataSram()
  })

  val valid = io.info.valid
  val op    = io.info.op
  val rt    = io.src_info.src2_data
  val is_s  = io.info.src2_ren
  val addr  = io.src_info.src1_data + io.info.imm

  val wen   = Wire(UInt(DATA_SRAM_WEN_WID.W))
  val wdata = Wire(UInt(DATA_SRAM_DATA_WID.W))

  wen   := 0.U
  wdata := 0.U

  when (valid && is_s) {
    switch (op) {
      is (LSUOpType.sb) {
        wen   := "b0000_0001".U << addr(2, 0)
        wdata := Fill(8, rt(7, 0))
      }
      is (LSUOpType.sh) {
        wen   := "b0000_0011".U << addr(2, 0)
        wdata := Fill(4, rt(15, 0))
      }
      is (LSUOpType.sw) {
        wen   := "b0000_1111".U << addr(2, 0)
        wdata := Fill(2, rt(31, 0))
      }
      is (LSUOpType.sd) {
        wen   := "b1111_1111".U << addr(2, 0)
        wdata := Fill(1, rt(63, 0))
      }
    }
  }

  io.dataSram.en    := !reset.asBool
  io.dataSram.wen   := wen
  io.dataSram.addr  := addr
  io.dataSram.wdata := wdata

}
```

实现和数据存储器 `DataMEM` 及其接口 `DataSram` 交互的 `FU` 部件 `LSU`，这里主要实现存数部分的功能。

这个部分要细致阅读文档中关于 `wen` 和 `wdata` 的设置问题，不然会挂的很惨。

`playground/src/pipeline/memory/MemoryUnit.scala`：

```scala
    // LAB4: MemoryUnit : Input loadData
    val loadData       = Input(UInt(XLEN.W))

	// LAB4: MemoryUnit : Finish Load
  val data  = io.memoryStage.data

  val valid = data.info.valid
  val fusel = data.info.fusel
  val is_l  = !data.info.src2_ren
  val op    = data.info.op
  val addr  = data.src_info.src1_data + data.info.imm
  val rdata = io.loadData >> (addr(2, 0) * 8.U)

  val res   = Wire(UInt(XLEN.W))
  
  res := data.rd_info.wdata

  when (valid && fusel === FuType.lsu && is_l) {
    switch (op) {
      is (LSUOpType. lb) { res := Cat(Fill(56, rdata( 7)), rdata( 7, 0)) }
      is (LSUOpType. lh) { res := Cat(Fill(48, rdata(15)), rdata(15, 0)) }
      is (LSUOpType. lw) { res := Cat(Fill(32, rdata(31)), rdata(31, 0)) }
      is (LSUOpType. ld) { res := rdata }
      is (LSUOpType.lbu) { res := rdata( 7, 0) }
      is (LSUOpType.lhu) { res := rdata(15, 0) }
      is (LSUOpType.lwu) { res := rdata(31, 0) }
    }
  }
  
  io.writeBackStage.data.rd_info.wdata := res
```

实现和数据存储器 `DataMEM` 以及其接口 `DataSram` 交互的 `FU` 部件 `LSU`，这里主要实现取数部分的功能。

这里实际上应该多实现一个 `LSU.scala`，但是 `MemoryUnit.scala` 里好像也没实现啥其它很多功能就直接塞进里面了。

由于这个部分实际上不需要往数据存储器 `DataMEM` 及其接口 `DataSram` 写入，所以直接在 `core` 里把 `wdata` 丢进来就行了，这里被重命名成了 `loadData`。

同样的，这个部分要细致阅读文档中关于 `wdata` 的设置问题，不然会挂的很惨。

`playground/src/Core.scala`：

```scala
	// LAB4: Memory
  memoryUnit.loadData := io.dataSram.rdata
```

这里需要新增一条数据通路，即将 `wdata` 传入 `MemoryUnit`。

这个部分其实考虑了好久，因为之前一直在用程序设计语言的思路考虑这个传输，但实际上这是电路，只要保证有一条数据通路可以把 `wdata` 从 `ExecuteUnit` 里接到 `MemoryUnit` 里就可以了。

但是要注意一点，这条数据通路上不能有额外的寄存器，因为取数时数据存储器 `DataMEM` 里自带一个时钟的延迟，所以这个 `wdata` 不能接入 `MemoryStage` 而要直连 `MemoryUnit`。

## Lab4 - report

鸽。

## Lab4 - Thinking & Exploration

鸽。

## Lab5 - Code

`playground/src/defines/isa/Instructions.scala`：

```scala
// LAB5: FuType
object FuType {
  def num     = 4
  def alu     = 0.U
  def mdu     = 1.U
  def lsu     = 2.U
  def bru     = 3.U
  def apply() = UInt(log2Up(num).W)
}

// LAB5: BRUOpType
object BRUOpType {

  def beq  = 0.U
  def bne  = 1.U
  def blt  = 2.U
  def bge  = 3.U
  def bltu = 4.U
  def bgeu = 5.U
  def jal  = 6.U
  def jalr = 7.U

}
```

`playground/src/defines/isa/RVI.scala`：

```scala
// LAB5: RV32I_BRUInstr
object RV32I_BRUInstr extends HasInstrType with CoreParameter {

  def BEQ  = BitPat("b???????_?????_?????_000_?????_1100011")
  def BNE  = BitPat("b???????_?????_?????_001_?????_1100011")
  def BLT  = BitPat("b???????_?????_?????_100_?????_1100011")
  def BGE  = BitPat("b???????_?????_?????_101_?????_1100011")
  def BLTU = BitPat("b???????_?????_?????_110_?????_1100011")
  def BGEU = BitPat("b???????_?????_?????_111_?????_1100011")

  def JAL  = BitPat("b????????????????????_?????_1101111")
  def JALR = BitPat("b????????????_?????_000_?????_1100111")

  val table = Array(

    BEQ  -> List(InstrB, FuType.bru, BRUOpType.beq),
    BNE  -> List(InstrB, FuType.bru, BRUOpType.bne),
    BLT  -> List(InstrB, FuType.bru, BRUOpType.blt),
    BGE  -> List(InstrB, FuType.bru, BRUOpType.bge),
    BLTU -> List(InstrB, FuType.bru, BRUOpType.bltu),
    BGEU -> List(InstrB, FuType.bru, BRUOpType.bgeu),

    JAL  -> List(InstrJ, FuType.bru, BRUOpType.jal),
    JALR -> List(InstrI, FuType.bru, BRUOpType.jalr),

  )

}

object RVIInstr extends CoreParameter {
  val table = RV32I_ALUInstr.table ++
    (if (XLEN == 64) RV64IInstr.table else Array.empty) ++
    // LAB3: RVIInstr : RV32MInstr & RV64MInstr
    RV32MInstr.table ++
    (if (XLEN == 64) RV64MInstr.table else Array.empty) ++
    // LAB4: RVIInstr : RV32I_LSUInstr
    RV32I_LSUInstr.table ++
    // LAB5: RVIInstr : RV32I_BRUInstr
    RV32I_BRUInstr.table
}
```

`playground/src/defines/Bundles.scala`：

```scala
// LAB5: FetchInfo
class FetchInfo extends Bundle {
  val branch = Bool()
  val target = UInt(XLEN.W)
}
```

由于要新增一条从 `ExecuteUnit` 到 `FetchUnit` 的数据通路，并且传输的数据有两个 `branch` 和 `target`，所以把这俩打包成 `Bundle` 并命名为 `FetchInfo`。

`playground/src/pipeline/fetch/FetchUnit.scala`：

```scala
    // LAB5: FetchUnit New Input : ftcInfo : branch & target
    val ftcInfo     = Input(new FetchInfo())

	// LAB5: FetchUnit : update pc_next
  when (io.ftcInfo.branch) {
    io.instSram.addr := io.ftcInfo.target
  }
```

由于新增了数据通路 `ftcInfo`，需要给 `FetchUnit` 新增输入通路。

当执行跳转指令时，下一条指令的地址 `pc` 需要根据当前指令的的执行结果得到，所以需要根据新增数据通路 `ftcInfo` 处理。

这里看到处理方式的时候就想到了中间处于 `Decode` 环节的指令怎么办的问题，然后看了文档说跳转指令后插无用指令，再看了下差分测试对应的汇编 `lab5.asm` 发现还真插了两条 `addi x0,x0,0`。

这里之前还瞎写成：

```scala
  io.instSram.addr := (if (io.ftcInfo.branch == true.B) io.ftcInfo.target else pc + 4.U)
```

由于 `chisel` 里面 `==` 和 `===` 完全不是一回事，大概前者是逻辑比较而后者是电路实体比较（会生成比较电路），导致这条数据通路其实压根没用。后面 `gtkwave` 看波形图发现根本没 `ftcInfo` 这么个信号，打开生成的 `verilog` 代码也发现查找不到 `ftcInfo` 相关的电路，才知道大概是因为编译过程中被识别为无效电路优化掉了，最后发现大概是这么个情况。

`playground/src/pipeline/decode/Decoder.scala`：

```scala
  // LAB5: Decoder : src1_pcen
  io.out.info.src1_pcen  := (inst === BitPat("b????????????????????_?????_0010111")) || (instrType === InstrJ)

    // LAB5: Decoder : imm : InstrB & InstrJ
    is (InstrB) {
      val imm13 = Cat(Cat(inst(31), inst(7)), Cat(inst(30, 25), inst(11, 8))) << 1
      imm := Cat(Fill(53, imm13(12)), imm13)
    }
    is (InstrJ) {
      val imm21 = Cat(Cat(inst(31), inst(19, 12)), Cat(inst(20), inst(30 ,21))) << 1
      imm := Cat(Fill(43, imm21(20)), imm21)
    }
```

这里不止 `imm` 要新增两种指令类型 `B` 型和 `J` 型，`rs1` 的来源为 `pc` 的条件 `src1_pcen` 也要新增 `J` 型指令的情况。

`playground/src/pipeline/execute/ExecuteUnit.scala`：

```scala
    // LAB5: ExecuteUnit New Output : ftcInfo
    val ftcInfo      = Output(new FetchInfo())

  // LAB5: ExecuteUnit : ftcInfo
  io.ftcInfo <> fu.ftcInfo
```

新增数据通路 `ftcInfo`，`ExecuteUnit` 负责中转。

`playground/src/pipeline/execute/Fu.scala`：

```scala
    // LAB5: Fu New Output : ftcInfo
    val ftcInfo  = Output(new FetchInfo())

    // LAB5: New FU : BRU
    is (FuType.bru) {
      val bru = Module(new Bru()).io
      bru.info     := io.data.info
      bru.src_info := io.data.src_info
      bru.pc       := io.data.pc
      bru.ftcInfo  <> io.ftcInfo
      res          := bru.result
    }
```

由于新增了数据通路 `ftcInfo`，需要给 `FU` 新增输出通路。

`BRU` 和 `FU` 的交互是目前最复杂的：不仅有新增的数据通路 `ftcInfo`，还有结果返回，还需要输入 `pc`。

`playground/src/pipeline/execute/fu/Bru.scala`：

```scala
// LAB5: BRU Module

package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class Bru extends Module {
  val io = IO(new Bundle {
    val info     = Input(new Info())
    val src_info = Input(new SrcInfo())
    val pc       = Input(UInt(XLEN.W))
    val ftcInfo  = Output(new FetchInfo())
    val result   = Output(UInt(XLEN.W))
  })
  
  val valid  = io.info.valid
  val op     = io.info.op
  val rs     = io.src_info.src1_data
  val rt     = io.src_info.src2_data
  val pc     = io.pc
  val imm    = io.info.imm
  val new_pc = pc + imm

  val branch = Wire(Bool())
  val target = Wire(UInt(XLEN.W))
  val res    = Wire(UInt(XLEN.W))

  branch := false.B
  target := 0.U
  res    := 0.U
  
  when (valid) {
    switch (op){
      is (BRUOpType.beq) {
        when (rs === rt) {
          branch := true.B
          target := new_pc
        }
      }
      is (BRUOpType.bne) {
        when (rs =/= rt) {
          branch := true.B
          target := new_pc
        }
      }
      is (BRUOpType.blt) {
        when (rs.asSInt < rt.asSInt) {
          branch := true.B
          target := new_pc
        }
      }
      is (BRUOpType.bge) {
        when (rs.asSInt >= rt.asSInt) {
          branch := true.B
          target := new_pc
        }
      }
      is (BRUOpType.bltu) {
        when (rs < rt) {
          branch := true.B
          target := new_pc
        }
      }
      is (BRUOpType.bgeu) {
        when (rs >= rt) {
          branch := true.B
          target := new_pc
        }
      }
      is (BRUOpType.jal) {
        branch := true.B
        target := new_pc
        res    := pc + 4.U
      }
      is (BRUOpType.jalr) {
        branch := true.B
        target := (rs + imm) & Cat(Fill(63,"b1".U),"b0".U)
        res    := pc + 4.U
      }
    }
  }

  io.ftcInfo.branch := branch
  io.ftcInfo.target := target
  io.result         := res

}
```

对着 RISC-V 手册写就好了，这里没有什么难点。

`playground/src/Core.scala`：

```scala
  // LAB5: FetchUnit
  executeUnit.ftcInfo <> fetchUnit.ftcInfo
```

这里需要新增一条数据通路，即将 `ftcInfo` 从 `ExecuteUnit` 传入 `FetchUnit`。

## Lab5 - report

鸽。

## Lab5 - Thinking & Exploration

鸽。

