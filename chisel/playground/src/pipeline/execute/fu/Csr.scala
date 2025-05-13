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

    // LAB9: Csr New Interaction : ExceptionInfo & mret
    val ex_in     = Input(new ExceptionInfo())
    val ex_out    = Output(new ExceptionInfo())
    val mret      = Output(new Bool())
  })

  // LAB9: Csr : Initialize ExceptionInfo & mret
  io.ex_out := io.ex_in
  io.mret   := false.B

  io.csr_read.raddr  := io.info.csr

  // io.csr_write.wen   := false.B
  io.csr_write.waddr := io.info.csr

  val valid = io.info.valid
  val op    = io.info.op
  val rs    = io.src_info.src1_data
  val cdata = io.csr_read.rdata

  io.result := cdata

  val wdata = Wire(UInt(XLEN.W))

  wdata := 0.U

  // LAB9: Initialize ren & wen
  io.csr_read.ren  := false.B
  io.csr_write.wen := false.B

  when (valid) {

    // io.csr_write.wen := true.B

    switch (op){
      is (CSROpType.csrrw) { wdata := rs }
      is (CSROpType.csrrs) { wdata := cdata | rs }
      is (CSROpType.csrrc) { wdata := cdata & ~rs }
      is (CSROpType.mret) { io.mret := true.B }
    }

    // LAB9: illegalInst : read / write csr
    val waddr   = io.info.reg_waddr
    val is_csri = io.info.is_csri
    val zimm    = io.info.zimm
    val rs_addr = io.info.src1_raddr
    val inst    = io.info.inst
    val ren     = Wire(Bool())
    val wen     = Wire(Bool())

    ren := false.B
    wen := false.B

    when (op === CSROpType.csrrw) {
      ren := waddr =/= 0.U
      wen := true.B
    }.elsewhen (op === CSROpType.csrrs || op === CSROpType.csrrc) {
      ren := true.B
      wen := Mux(is_csri, zimm, rs_addr) =/= 0.U
    }

    io.csr_read.ren  := ren
    io.csr_write.wen := wen

    when (!io.csr_read.rlegal || !io.csr_write.wlegal) {
      io.ex_out.exception(illegalInst) := true.B
      io.ex_out.tval(illegalInst)      := inst
    }

  }

  io.csr_write.wdata := wdata

}
