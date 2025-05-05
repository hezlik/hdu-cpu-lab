// LAB8: CSR Module

package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class Csr extends Module {
  val io = IO(new Bundle {
    val info      = Input(new Info())
    val src_info  = Input(new SrcInfo())
    val result    = Output(UInt(XLEN.W))

    val csr_read  = new CsrRead()
    val csr_write = new CsrWrite()
  })

  io.csr_read.raddr  := io.info.csr

  io.csr_write.wen   := false.B
  io.csr_write.waddr := io.info.csr

  val valid = io.info.valid
  val op    = io.info.op
  val rs    = io.src_info.src1_data
  val csr   = io.csr_read.rdata 

  io.result := csr

  val wdata = Wire(UInt(XLEN.W))

  wdata := 0.U

  when (valid) {
    io.csr_write.wen := true.B
    switch (op){
      is (CSROpType.csrrw) { wdata := rs }
      is (CSROpType.csrrs) { wdata := csr | rs }
      is (CSROpType.csrrc) { wdata := csr & ~rs }
    }
  }

  io.csr_write.wdata := wdata

}
