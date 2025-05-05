## environment

https://code.educoder.net/ppg69fuwb/riscv-lab

此处直接看项目的 README 即可。

## Lab1 - Code

每一个地方的修改有对应 LABx 的注释，可以利用 TODO Tree 快速定位查看。

`playground/src/defines/isa/Instructions.scala`

先直接顺序编号，后续可以再修改，其它模块只会使用 `ALUOpType.add` 形式的别名而不会使用具体的数字，不会出现连带修改的问题。

`playground/src/defines/isa/RVI.scala`

这个地方实际上最后会将所有指令列表连接成一张总表 `RVIInstr.table`。

之后在 `decode` 阶段，我们需要用到这张总表来做模式匹配分析出指令类型、指令使用的 `FU` 部件和指令使用的运算编号，那时候这里的 `RVIInstr.table` 会被重载为 `Instructions.DecodeTable`。

`playground/src/pipeline/decode/ARegfile.scala`。

这里 lab1 需要寄存器初始化 `regs(i) = i`，但是写丑了，直接手动了。lab2 开始这个部分要改回 `regs(i) = 0`。

写寄存器堆部分一开始忘记判地址是不是 `0` 了，`regs(0) = 0` 是 isa 标准规定，但没有怎么影响实验进度，啥时候想起来就看了一眼改了一下。

`playground/src/pipeline/decode/Decoder.scala`。

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

`playground/src/pipeline/decode/DecodeUnit.scala`

这里就是连接一下寄存器堆对指令进行进一步解析，实际上就是从寄存器堆中读出两个操作数传给 `execute` 模块。

`playground/src/pipeline/execute/ExecuteStage.scala`

这个部分对着 RISC-V 手册写，不要瞎搞就行。

为了更好的处理 RV64 中带 `W` 后缀的指令（也就是 $32$ 位运算指令），我们实现了一个函数 `W` 来实现 $32$ 位到 $64$ 位的有符号扩展操作，这使得 `ALU` 的实现更加简洁易懂，同时提高了代码复用性。

`playground/src/pipeline/execute/ExecuteUnit.scala`

把该传的传一下就行。

这里了解了一下 `=` 和 `:=` 的区别，在 chisel 中 `=` 是用于变量声明和初始化，`:=` 用于硬件连接赋值。

`playground/src/pipeline/memory/MemoryStage.scala`

各个 `Stage` 的作用就是暂存后传一下 `data`，很容易的。

注意这里的 `data` 是寄存器类型，这样才能实现时钟控制流水线。

`playground/src/pipeline/writeback/WriteBackStage.scala`

`playground/src/pipeline/writeback/WriteBackUnit.scala`

这里要注意把 `debug` 信息传出去。

`playground/src/Core.scala`

根据整个流水线的图来看怎么连接总线就很容易了。

注意这里需要特别处理寄存器堆 `regfile` 和数据存储器 `dataSram` 的一些事情，以及把 `debug` 丢出去的事情。

## Lab1 - Report

1. 选择表 9-1 中的一条指令（非 `add`)，按照你自已的理解，逐步介绍其数据通路设计的思路以及实现过程。

   鸽。

2. 尝试自己绘制一幅 MyCPU 内部数据通路图，后续实验将在此基础上修改，使得该 MyCPU 能够执行更多格式的指令。

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

   实际上所有 `W` 指令都可以通过一些非 `W` 指令的组合实现，`W` 指令本身是为了从硬件层面上在 RV64 中优化 $32$ 位数据的运算。不同的 `R` 型 `W` 指令的组合难度不同，困难的就从硬件层面实现，简单的就不实现，通常是需要符号扩展才实现。

   具体而言分为 $4$ 类指令：

   1. `add, sub` 等数值运算指令：有符号情况下 $32$ 位数据的符号位在第 $31$ 位，会涉及到符号扩展的问题，需要特殊实现 `W` 指令。
   2. `slt, sltu` 等比较运算指令：通过一些截断和反转位操作，$32$ 位和 $64$ 位比较实际上没有区别，实现难度较小且不需要对结果做符号扩展，不需要特殊实现 `W` 指令。
   3. `and, xor` 等位运算指令：位运算天然的每位独立，$32$ 位和 $64$ 位位运算实际上没有区别，不需要特殊实现 `W` 指令。
   4. `sll, sra` 等移位指令：$32$ 位数据需要限制 `rs2` 只有低 `5` 位有效，同时也存在符号扩展问题，需要特殊实现 `W` 指令。

7. 请问差分测试框架只用图 9-11 中的 $4$ 个 `debug` 信号够吗？假如有的指令不将结果写回通用寄存器，这时框架该如何发现问题？

   个人认为不太够。`R` 型指令都在通用寄存器操作这样做问题不大，但在后续出现能够操作内存的指令时，这样的 $4$ 个 `debug` 信号并不足够，如果后续 lab 中不能改动的话可能需要细致的设计测试用例等手段来实现更为准确的差分测试。

8. 当前处理器采用的是哈佛结构还是冯诺依曼结构？

   哈佛结构。lab1 文档中明确指出指令存储器`instMEM` 和数据存储器 `dataMEM` 存放在两个完全独立的 SRAM 模块中，数据存储和指令存储分开是典型的哈佛结构。

## Lab2 - Code

`playground/src/defines/Bundles.scala`

给 `Info` 中增加一些可以传递的信息，四个项分别是：

1. `src1_ren`：第一个操作数是否要读寄存器
2. `src2_ren`：第二个操作数是否要读寄存器
3. `imm`：立即数
4. `src1_pcen`：第一个操作数是否要读 `pc`

虽然 lab2 文档中推荐把整条指令 `inst` 也打包进去，但我没有这么做。主要是因为我认为这样子会混淆 `Decoder` 和 `DecodeUnit` 要做的事情：`Decoder` 负责解析出所有可以从指令本身解析出的东西，`DecodeUnit` 负责根据 `Decoder` 提供的信息读寄存器和 `pc`。

`playground/src/defines/isa/RVI.scala`

增加了一些 `I` 型和 `U` 型指令，没什么好说的。

`playground/src/pipeline/decode/ARegfile.scala`

lab2 开始这个部分改回 `regs(i) = 0`。

`playground/src/pipeline/decode/Decoder.scala`

`src1_ren` 和 `src2_ren` 对着 RISC-V 手册很容易写出来。

`src1_pcen` 暂且只知道有 `auipc` 指令需要读 `pc`，后续可能要修改。

`imm` 目前对着 RISC-V 手册只解析了 `I` 型和 `U` 型，后续用到了其它类型再加。

`playground/src/pipeline/decode/Decoder.scala`

通过多路选择器得到 `src1_data` 和 `src2_data`，从而实现 `ALU` 部件的复用。

这里使用多路选择器处理选择读寄存器、`pc`、立即数还是 `0`，估计后面还得接着加。

`playground/src/pipeline/execute/fu/Alu.scala`

这里在 lab1 的时候实现有误，但因为没有立即数相关的指令这些问题很难卡出来所以被拖到了 lab2 才被卡出来。

具体其实就是没有完全对着 RISC-V 手册写导致的，主要是 `W` 指令位移 `rs2` 要取低 $5$ 位而不是 $6$ 位，以及 `rs1` 要直接先截断成 $32$ 位。

调试的时候在汇编文件 `lab2.asm` 中读到差分测试出错的指令是这几条位运算，第一反应就是直接重新读一遍 RISC-V 手册，大概率是实现和手册不一致导致的这类问题，所以也没有用上 gtkwave 啥的调试工具。

这里还有一个问题，在老版的 RISC-V 手册中 `srai` 等指令要求 `imm` 的第 $5$ 位必须是 $0$ 才能有效，然而查阅最新的手册发现，现在这个问题已经变成一个可选的情况了。事实上这个问题在模式匹配 `BitPat` 中是按照老版写了的。

## Lab2 - Report

1. 选择这次实验中添加的指令中的一条，按照你自己的理解，逐步介绍其数据通路设计的思路以及实现过程。

   鸽。

2. 更新上一实验中绘制完成的数据通路图。

   鸽。

3. 谈谈你对数据通路复用的理解。

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

`playground/src/defines/isa/Instructions.scala`

这里新增一种 `FU` 部件即 `MDU`，专门用来处理乘除法运算指令，即 RV32/64M 指令集。

`playground/src/defines/isa/RVI.scala`

模仿之前的 `RV32I_ALUInstr` 和 `RV64IInstr` 写就可以了，注意检查一下不要抄错 RISC-V 手册。

这里这样搞其实命名就不太对，应该把 `RVIInstr` 改成 `RVInstr`，不过这个地方改了还得改后面的 `Decoder.scala`，有点麻烦且无伤大雅就没改。

`playground/src/defines/Bundles.scala`

就新加一个 `fusel` 表示使用哪个 `FU` 部件就行了。

`playground/src/pipeline/decode/Decoder.scala`

`Decoder.scala` 中也需要提取出使用哪个 `FU` 部件的信息。

`playground/src/pipeline/execute/Fu.scala`

此时需要在 `Fu.scala` 中实现按照 `fusel` 选择对应 `FU` 部件的功能。

这里注意到一个事情，我们没有在这里处理 `io.data.info.valid` 的事情，事实上在电路中只要有任意一层处理 `valid` 就行了，因为任意一层断路都可以做到让这个电信号传不到 `ExecuteUnit`。这里将 `valid` 的处理直接丢给了底层的 `LU` 分部件（即 `ALU` 之类的）。

`playground/src/pipeline/execute/fu/Mdu.scala`

这个具体实现 `MDU` 的部分有很多问题，主要就是不太了解 RISC-V 手册中的乘除法和 `scala` 中的乘除法在各种情况下的结果，最后情况都是试出来的。

首先我先假设 `scala` 中的乘除法和 RISC-V 手册中的乘除法行为一致，写了第一个版本。然后反复差分测试，发现问题主要在于除法：

1. 除法的除数为 $0$ 时，需要让商为整型最大值，余数为被除数（但实际上 RISC-V 手册中没有规定除 $0$ 应该给出什么结果）。
2. 有符号取余要通过被除数 $-$ 商 $\times$ 除数实现。

我觉得这个部分的测试应该不是很强，大概率写的代码还有问题，但是问题可以先交给以后的我！~~估计乘除法部分后面也不太会有测试。~~

## Lab3 - Report

1. 选择需要实现的指令中的一条，按照你自己的理解，逐步介绍其数据通路设计的思路以及实现过程。

   鸽。

2. 更新本章实验二中绘制完成的数据通路图。

   鸽。

## Lab3 - Thinking & Exploration

1. 为什么乘法指令中只有 `mul` 有对应的字指令，而别的乘法指令没有？

   主要应该是由于应用场景的原因。通常来说乘法有两种实际情况：截断取低位结果和获取完整结果。对于在 $64$ 位机和指令集下 $32$ 位整型的乘法，前者只需要实现 `mul` 对应的字指令 `mulw`，后者可以通过符号扩展后直接 `mul` 获得完整的 $64$ 位结果，故不需要特别实现其余三条高位乘法指令对应的字指令。

2. 用一条指令将 $128$ 位的积写入两个 $64$ 位寄存器会增加硬件复杂度，能否从数据通路复用的角度解释一下原因呢？

   首先 $128$ 位积写入两个 $64$ 位寄存器需要在指令中额外增加结果寄存器地址，这就不是一条标准的 `R` 型指令了，无法复用 `R` 型指令的数据通路设计。

   其次就算指定两个结果 $64$ 位寄存器地址的分布方式（比如说前后两个），仍然可以归为标准 `R` 型指令。其它 `R` 型指令的结果都只有 $64$ 位需要 $64$ 根线，而乘法指令需要 $128$ 位 $128$ 根线，为了乘法指令需要单独另外增加 $128$ 根线增加硬件复杂度。此外之后的结果写入寄存器还需要判定是否是乘法指令，不能直接复用其它 `R` 型指令的结果写入数据通路而需要单独增加数据通路和数据选择器，更是增加了硬件复杂度。

## Lab4 - Code

`playground/src/defines/isa/Instructions.scala`

`playground/src/defines/isa/RVI.scala`

`playground/src/pipeline/decode/Decoder.scala`

`Decoder` 中对立即数 `imm` 的解析增加 `S` 型指令。

`playground/src/pipeline/execute/Fu.scala`

新增部件 `LSU`，注意这里的电路连接与之前的 `ALU` 和 `MDU` 并不完全相同，`LSU` 不返回结果 `result`，但需要接入 `dataSram`。

`playground/src/pipeline/execute/fu/Lsu.scala`

实现和数据存储器 `DataMEM` 及其接口 `DataSram` 交互的 `FU` 部件 `LSU`，这里主要实现存数部分的功能。

这个部分要细致阅读文档中关于 `wen` 和 `wdata` 的设置问题，不然会挂的很惨。

`playground/src/pipeline/memory/MemoryUnit.scala`

实现和数据存储器 `DataMEM` 以及其接口 `DataSram` 交互的 `FU` 部件 `LSU`，这里主要实现取数部分的功能。

这里实际上应该多实现一个 `LSU.scala`，但是 `MemoryUnit.scala` 里好像也没实现啥其它很多功能就直接塞进里面了。

由于这个部分实际上不需要往数据存储器 `DataMEM` 及其接口 `DataSram` 写入，所以直接在 `core` 里把 `wdata` 丢进来就行了，这里被重命名成了 `loadData`。

同样的，这个部分要细致阅读文档中关于 `wdata` 的设置问题，不然会挂的很惨。

`playground/src/Core.scala`

这里需要新增一条数据通路，即将 `wdata` 传入 `MemoryUnit`。

这个部分其实考虑了好久，因为之前一直在用程序设计语言的思路考虑这个传输，但实际上这是电路，只要保证有一条数据通路可以把 `wdata` 从 `ExecuteUnit` 里接到 `MemoryUnit` 里就可以了。

但是要注意一点，这条数据通路上不能有额外的寄存器，因为取数时数据存储器 `DataMEM` 里自带一个时钟的延迟，所以这个 `wdata` 不能接入 `MemoryStage` 而要直连 `MemoryUnit`。

## Lab4 - Report

1. 仿照表 9-18，列出 `sh, sw, sd` 的写地址与字节写使能的对应关系。

   鸽。

2. 选择第 6 章的 6.4 小节访存指令中的一条不同于 `lh` 和 `sb` 的指令，按照自己的理解，逐步介绍其数据通路设计的思路以及实现过程。

   鸽。

3. 修改 MyCPU 内部数据通路图，增加访存功能。

   鸽。

## Lab4 - Thinking & Exploration

1. 在梳理写地址和字节写使能对应关系时是否遇到了问题？RISC-V 定义了地址未对齐异常，在该问题上对你有什么启发？

   暂时没啥问题，就是通过写地址后三位判断判断写的位置的最低位在哪。这种对异常行为作出规定的操作在工程中非常常见，实际上是将异常处理问题转移给了其他层次（如软件层或用户）。事实上 RISC-V 手册规定了可以选择是否实现地址未对齐情况下的存数指令，用户处理这个问题的时候应该参考具体的机器配置和实现（当然最好的办法是直接让这些异常不会出现）。

2. 数据存储器使能信号 `DataMEM_en` 可以不恒为 $1$ 吗？这样可以降低 CPU 功耗。如果可以，应该怎么修改？

   可以。一个可能的实现是在需要存数或取数时才使 `DataMEM_en` 为 $1$ 否则为 $0$，具体实现疑似比较复杂，因为存取操作是跨周期的需要让 `DataMEM_en` 能够延迟关闭，一个可行的办法是在执行单元和访存单元同时实现数据选择器。

   但是这样是否能够降低 CPU 功耗存疑，因为启动数据存储器的功耗不见得小，当频繁执行存取指令的时候 CPU 功耗反而可能升高。

3. 表 9-20 展示了读取同一个 $8$ 字节数据块的情况，假如需要跨块访问会发生什么情况？RISC-V 是怎么解决这种问题的？可以和 MIPS 进行对比举例。

   RISC-V 中对跨块访问的支持是可选的，所以会有两种情况：
   
   - 支持跨块访问：硬件会自动将其拆分为多条取数指令。
   - 不支持跨块访问：直接出发地址未对齐异常，剩下的事情交由软件层处理。
   
   作为对比，MIPS 强制要求硬件支持跨块访问。此时 RISC-V 和 MIPS 的行为仍然不同：MIPS 有专用的读左侧字 `lwl` 和读右侧字`lwr`指令，并且两条指令都只部分写入寄存器，从而增加访问寄存器的硬件复杂度；RISC-V 则仅有读字指令 `lw`，将拆分后的指令读取结果分别存入两个寄存器之后拼接结果，不会增加访问寄存器的硬件复杂度，但相比 MIPS 而言需要更多的指令数（拼接过程）。

## Lab5 - Code

`playground/src/defines/isa/Instructions.scala`

`playground/src/defines/isa/RVI.scala`

`playground/src/defines/Bundles.scala`

由于要新增一条从 `ExecuteUnit` 到 `FetchUnit` 的数据通路，并且传输的数据有两个 `branch` 和 `target`，所以把这俩打包成 `Bundle` 并命名为 `FetchInfo`。

`playground/src/pipeline/fetch/FetchUnit.scala`

由于新增了数据通路 `ftcInfo`，需要给 `FetchUnit` 新增输入通路。

当执行跳转指令时，下一条指令的地址 `pc` 需要根据当前指令的的执行结果得到，所以需要根据新增数据通路 `ftcInfo` 处理。

这里看到处理方式的时候就想到了中间处于 `Decode` 环节的指令怎么办的问题，然后看了文档说跳转指令后插无用指令，再看了下差分测试对应的汇编 `lab5.asm` 发现还真插了两条 `addi x0,x0,0`。

这里之前还瞎写成：

```scala
  io.instSram.addr := (if (io.ftcInfo.branch == true.B) io.ftcInfo.target else pc + 4.U)
```

由于 `chisel` 里面 `==` 和 `===` 完全不是一回事，大概前者是逻辑比较而后者是电路实体比较（会生成比较电路），导致这条数据通路其实压根没用。后面 `gtkwave` 看波形图发现根本没 `ftcInfo` 这么个信号，打开生成的 `verilog` 代码也发现查找不到 `ftcInfo` 相关的电路，才知道大概是因为编译过程中被识别为无效电路优化掉了，最后发现大概是这么个情况。

`playground/src/pipeline/decode/Decoder.scala`

这里不止 `imm` 要新增两种指令类型 `B` 型和 `J` 型，`rs1` 的来源为 `pc` 的条件 `src1_pcen` 也要新增 `J` 型指令的情况。

`playground/src/pipeline/execute/ExecuteUnit.scala`

新增数据通路 `ftcInfo`，`ExecuteUnit` 负责中转。

`playground/src/pipeline/execute/Fu.scala`

由于新增了数据通路 `ftcInfo`，需要给 `FU` 新增输出通路。

`BRU` 和 `FU` 的交互是目前最复杂的：不仅有新增的数据通路 `ftcInfo`，还有结果返回，还需要输入 `pc`。

`playground/src/pipeline/execute/fu/Bru.scala`

对着 RISC-V 手册写就好了，这里没有什么难点。

`playground/src/Core.scala`

这里需要新增一条数据通路，即将 `ftcInfo` 从 `ExecuteUnit` 传入 `FetchUnit`。

## Lab5 - Report

1. 仿照本章实验一中的取指令访问时序波形图，绘制发生跳转前后的取指令访问时序波形图。

   鸽。

2. 选择第 6 章的 6.4 小节转移指令中的一条指令（非 `beq`、非 `jalr`），按照你自己的理解，逐步介绍其数据通路设计的思路以及实现过程。

   鸽。

3. 修改 MyCPU 内部数据通路图，增加转移功能。

   鸽。

## Lab5 - Thinking & Exploration

1. 为什么 `jalr` 的 `target` 需要与上 `~1.U(XLEN.W)`？其它转移指令为什么不需要这样的操作？

   因为 `pc` 随时要保持 $2$ 字节对齐（最低位强制为 $0$）。其它转移指令的两个相加参数都是 `pc` 和 `imm`（`B` 型指令和 `J` 型指令的 `imm` 最低位一定为 $0$）最低位均为 $0$， 只有`jalr` 的两个相加参数是 `rs1` 和 `imm` 最低位不一定是 $0$ 需要与上 `~1.U(XLEN.W)`。

   至于为什么是最低的 $1$ 位而不是 $2$ 位？因为 RISC-V 存在 RVC 的精简指令集可选扩展，其指令长度为 $2$ 个字节 $16$ 位。

2. 能否在译码单元实现转移指令？如果可以，应该怎样实现，需要考虑哪些因素？如果不可以，又是什么原因导致的？

   可以，但不建议。相当于需要把 `BRU` 挪到译码级，理想流水线中比较好改，非理想流水线还需要考虑控制冲突的处理略有不同，主要有以下的几个问题：

   - 转移指令需要做加法等运算，把 `BRU` 搬过来容易混淆各层级的职责划分。
   - 需要在译码单元中增加数据选择器，执行单元中的数据选择器也要更改。
   - 更容易产生控制冲突：当前跳转指令是否跳转可能取决于下一条指令的执行结果。

3. 如果是非理想流水线，转移指令的下一条指令已经在译码级，当转移成功时这已经在译码级的下一条指令是否应该作废？如果该作废，应该怎么处理？

   应该作废。作废可以通过将处于译码级的这条指令的 `valid` 赋值为 $0$ 或直接整个 `info` 清零，前者需要注意之前的几个 lab 实现要正确处理 `valid` 信息。

4. 观察绘制出的取指令时序波形图，你是否发现了什么问题？

   取指令中的 `pc` 并不会在对应跳转指令后直接改变成跳转地址，而是会继续加 $4$ 后两次才改变，这是流水线的特性决定的。

## Lab6 - Code - Centralized

`playground/src/defines/Bundles.scala`

把数据缓存的更新信号 `allow_to_go` 和清空信号 `do_flush` 打包。

`playground/src/pipeline/fetch/FetchUnit.scala`

用 `fetchUnit` 的控制信号 `fetchCtrlSignal` 控制 `PC` 更新，`allow_to_go` 不准走就不许更新 `PC`。

`playground/src/pipeline/decode/DecodeStage.scala`

用 `fetchUnit` 的控制信号 `fetchCtrlSignal` 控制 `decodeStage` 中的数据缓存的更新和清空。

清空可以直接使用 `valid := 0` 来实现，指令无效化就相当于清空指令。

后续的几个控制也是一样的。

`playground/src/pipeline/execute/ExecuteStage.scala`

`playground/src/pipeline/memory/MemoryStage.scala`

`playground/src/pipeline/writeback/WriteBackStage.scala`

`playground/src/Core.scala`

由于我是懒狗，直接把 `Ctrl` 集成到 `Core` 里了，本身 `Core` 在集成控制方面也有巨大优势，很多数据通路可以直接调用。

先不管具体的控制信号逻辑，那么对于流水线的相邻两个层级 `x` 和 `y`，它们之间的控制信号应该有如下两种必然关系：

1. `y` 的数据缓存更新被暂停了，那么 `x` 的数据传递会被 `y` 堵住，所以 `x` 也应该暂停数据缓存更新，即 `y_allow_to_go` 为 `false` 时相应的 `x_allow_to_go` 也为 `false`。
2. `x` 的数据缓存更新被暂停了，同时 `y` 的数据缓存更新没被暂停，下一周期 `y` 没有地方获取新数据，应该要清空数据缓存，即 `x_allow_to_go` 为 `false` 且 `y_allow_to_go` 为 `true` 时相应的 `x_do_flush` 为 `true`。（这里需要注意，`allow_to_go` 是管上一层级往当前缓存传递，`do_flush` 是管当前缓存往下一层级传递）。

除了这两种控制信号逻辑之外才有各种特化的冲突控制，所以这里对这两个部分做了分割。

在五级流水线中特化的冲突控制逻辑只有两种：

1. 写后读：处于 `Execute, Memory, WriteBack` 层级的指令写的寄存器和处于 `Decode` 层级的指令读的寄存器相同，此时需要暂停传递 `Decode` 层级的指令。
2. 控制冲突：处于 `Execute` 层级的跳转指令执行后，处于`Fetch, Decode` 层级的指令不应该被执行，此时需要清空处于这两个层级的指令。

具体的实现就按照文档中写。

获得了 `IPC: 0.720012` 的好成绩。

## Lab6 - Code - Federal

我的分布式实现是在每个流水线层级中额外添加了与 `Stage, Unit` 并列的控制模块 `Ctrl`。

整个 MyCPU 的控制信号应该有如下的控制逻辑（假设相邻两层依次是 `x,y`）：

1. `yStage` 控制 `x` 传给 `y`，且 `yStage` 中有寄存器做缓存。
2. 为了控制 `yStage` 中的传输，需要 `xCtrl` 产生控制信号 `x_allow_to_go` 和 `x_do_flush`。
3. 为了实现流水线暂停和插入气泡，需要在 `Ctrl` 模块之间传递 `Ready` 信号，`yReady` 表示层级 `y` 是否准备好了，也就是 `x` 层级的数据可以传输到 `y`。`xReady` 可以直接用 `x_allow_to_go` 赋值。
4. 此时仍然具有集中式中叙述的普适必然逻辑，只是需要用 `Ready` 信号转述。
5. 其余就是做特化逻辑。

`playground/src/pipeline/fetch/FetchUnit.scala`

`playground/src/pipeline/decode/DecodeStage.scala`

`playground/src/pipeline/execute/ExecuteStage.scala`

`playground/src/pipeline/memory/MemoryStage.scala`

`playground/src/pipeline/writeback/WriteBackStage.scala`

分布式中几个 `Stage` 和集中式控制是一样的，只需要传入对应控制信号并完成控制即可。

`playground/src/pipeline/fetch/FetchCtrl.scala`

需要额外传入 `ExcuteUnit` 中的 `ftcInfo` 出来给计算 `flush` 使用。

`playground/src/pipeline/decode/DecodeCtrl.scala`

需要额外传入 `excuteUnit` 中的 `ftcInfo` 出来给计算 `do_flush` 使用。

需要额外传入 `decodeUnit, excuteUnit, memoryUnit, writeBackUnit` 中的 `info` 出来给计算 `allow_to_go` 使用。

这里数据冲突实际上有大量重复的逻辑判断，可以打包成函数 `Conflict(r_info, w_info)` 来减少重复实现。

`playground/src/pipeline/execute/ExecuteCtrl.scala`

`playground/src/pipeline/memory/MemoryCtrl.scala`

`playground/src/pipeline/writeback/WriteBackCtrl.scala`

`playground/src/Core.scala`

`Core` 需要增加新的数据通路，包括几个 `Ready` 信号的传输、`Ctrl` 模块将控制信号传输到 `Stage` 模块、几个 `Info` 和 `ftcInfo` 的传输等。

同样获得了 `IPC: 0.720012` 的好成绩。

## Lab6 - Report

1. 仿照图 9-31 绘制集中式控制信号和指令时空图。

   鸽。

2. 修改 MyCPU 内部数据通路图，将理想流水线升级为气泡流水线。

   鸽。

## Lab6 - Thinking & Exploration

1. 读后读（RAR）冲突属于数据冲突吗？为什么？尝试结合图 9-26 分析一下。

   不属于数据冲突。当两条指令的源操作数相同时，不管以什么顺序读，这两条指令读出来的结果理论上相同，实际上也相同，不会发生读取错误。

2. 读后写冲突与写后写冲突对于顺序流水线无影响，其对于乱序流水线有影响吗？又该如何解决？尝试举例说明。

   鸽。

3. 对于超标量流水线而言，还可能存在什么样的结构冲突？对于这些冲突又可以如何解决？尝试举例说明。

   鸽。

4. 如何减轻由于分支导致的性能降低？可以从软件和硬件的角度进行分析。

   鸽。

5. 查阅 MIPS 相关资料，说说其对分支指令是如何进行优化的。

   鸽。

## Lab7 - Code

`playground/src/pipeline/decode/DecodeUnit.scala`

这部分和 lab6 的 `DecodeCtrl` 中的逻辑是一致的，也可以用函数封装一下，就是此时得判定是和 `src1` 冲突还是 `src2` 冲突。

`playground/src/pipeline/decode/DecodeCtrl.scala`

此时数据冲突只会在读数据存储器的时候发生了，也就是 `allow_to_go` 只有在译码层指令读的寄存器和执行层取数指令写的寄存器冲突时才为 `false`。

`playground/src/Core.scala`

`Core` 需要增加数据通路，调度几个 `Info` 和 `RdInfo` 到 `DecodeUnit` 中。

获得了 `IPC: 0.998522` 的好成绩。

## Lab7 - Report

1. 修改 MyCPU 内部数据通路图，为流水线引入数据前递功能。

   鸽。

## Lab7 - Thinking & Exploration

1. 相较于气泡流水线，本实验的 MyCPU 在引入数据前递功能后，性能有何提升？（可以从 IPC 角度进行说明）

   IPC 明显增加了，单个周期平均执行指令数增加了，性能明显提升。

   原因就是因为在数据冲突的时候本来需要插入气泡强行串行化，但是现在只有在取数指令造成的数据冲突下才插入气泡否则就直接读，平均下来单个周期执行的有效指令数明显增加。

2. 查阅资料，除数据前递技术外还可以如何改造气泡流水线提升性能？

   鸽。

3. 查阅资料，了解重定向流水线技术，其与数据前递技术有什么共同点和差异？

   鸽。

## Lab8 - Code

`playground/src/defines/Const.scala`

专门给 CSR 寄存器堆增加三个常数：

1. 寄存器个数 `CREG_NUM = 16`；
2. 虚拟寄存器地址宽度 `VT_CSR_ADDR_WID = 12`； 
3. 实际寄存器地址宽度 `CSR_ADDR_WID = 4`。

`playground/src/defines/Bundles.scala`

由于 CSR 的相应指令的译码和正常 `I` 型指令很不一样，故在 `Info` 包里加入了 `csr, is_csri, zimm` 三个字段。

`playground/src/defines/isa/Instructions.scala`

`playground/src/defines/isa/RVI.scala`

注意 CSR 指令仍然可以用到 lab2 中的数据通路复用的技巧。

`playground/src/pipeline/decode/Decoder.scala`

完成 `Info` 包里 `csr, is_csri, zimm` 的赋值逻辑。

`playground/src/pipeline/decode/DecodeUnit.scala`

给得到 `src1_data` 的多路选择器中增加输入，实现数据通路复用。

为了实现数据通路复用，我不得不给 `Info` 包里加了字段 `is_csri`。因为到 `DecodeUnit` 的时候我的 `Info` 包里已经把整条指令的信息 `inst` 丢了，而 `CSROpType` 也无法区分是不是读立即数的 CSR 指令，也就是说区分 CSR 指令读不读立即数需要在 `Decoder` 里完成。

这里 debug 了好久，最后看波形图发现立即数根本没有被放到 `src1_data` 里，才意识到发生了什么。

`playground/src/pipeline/execute/fu/CRegFile.scala`

这里要完成对 CSR 寄存器堆的维护，包括地址映射都在这一层完成，并提供读写的接口包 `CsrRead` 和 `CsrWrite`。

地址映射尝试了各种方案，包括文档里写的 `Map` 和之前写的 `ListLookup`：

1. `Map`：`chisel` 中形式为 `"hx".U` 的数值被认为是动态的值，而 `Map` 不特殊加库默认只支持不可更改的常 `Map`，所以调用 `getOrElse` 方法得到的一直是找不到的默认值 `0`，直接被忽略编译了数据通路。
2. `ListLookup`：不知道为什么被限制为键值只能是 `BitPat`。
3. `MuxLookup`：而且语法很奇怪，需要 `MuxLookup(key, default)(table)` 的形式，而且后面好像得自己打包，没法直接用 `List` 或者元组之类的东西，后面还出现了一大堆奇怪的 warning 然后过不了编译。

最后选择 `ListLookup`，前面强行写 `BitPat` 放虚拟地址，后面 `List` 里放实际地址和读写掩码。

然后是开寄存器堆，我的实现是先初始化全 $0$ 开 $16$ 个 CSR 寄存器（多加了一个 $0$ 寄存器用来当默认值），再给 `mstatus` 和 `misa` 赋初始值。~~然后这里手册开始锅锅锅了。~~

然后是读写寄存器，手册上写寄存器的逻辑有点问题，对于不可写位应该保持原样，应该写成：

```scala
csrs(waddr) := (io.write.wdata & wmask) | (csrs(waddr) & ~wmask)
```

`playground/src/pipeline/execute/fu/Csr.scala`

这里对着 RISC-V 手册写就可以了，地址映射没有放到这里做，交给了 CSR 寄存器堆 `csrfile`。

`playground/src/pipeline/execute/Fu.scala`

增加中继 `csr_read` 和 `csr_write` 的数据通路。

`playground/src/pipeline/execute/ExecuteUnit.scala`

增加中继 `csr_read` 和 `csr_write` 的数据通路。

`playground/src/Core.scala`

增加 `Execute` 和 `csrfile` 交互的数据通路，即把对应的 `csr_read` 和 `csr_write` 连接起来。

## Lab8 - Report

1. 选择一个 CSR 寄存器，介绍各字段的含义。

   鸽。

2. 选择第 6 章的 6.4 小节 CSR 指令中的一条指令（非 `csrrc`、非 `csrrwi`），按照你自己的理解，逐步介绍其数据通路设计的思路以及实现过程。

   鸽。

3. 修改 MyCPU 内部数据通路图，支持 CSR 指令。

   鸽。

## Lab8 - Thinking & Exploration

1. 查阅资料，哪些 CSR 寄存器常用 CSR 立即数指令进行读写操作？

   鸽。

2. CSR 指令的写（`write`）、置位（`set`）、清除（`clear`）分别在什么情况下使用？请结合例子说明。

   鸽。

3. 操作系统是怎么知道硬件的信息的呢？

   鸽。
