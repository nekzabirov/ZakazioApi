package com.zakaion.api.controller.payment

import com.google.gson.Gson
import com.zakaion.api.controller.BaseController
import com.zakaion.api.dao.BankCardDao
import com.zakaion.api.dao.TransactionInDao
import com.zakaion.api.dao.UserDao
import com.zakaion.api.entity.card.BankCardEntity
import com.zakaion.api.entity.transaction.TransactionInEntity
import com.zakaion.api.exception.BadParams
import com.zakaion.api.exception.NoPermittedMethod
import com.zakaion.api.exception.NotFound
import com.zakaion.api.factor.user.UserFactory
import com.zakaion.api.model.AddCardModel
import com.zakaion.api.model.AddPayModel
import com.zakaion.api.model.DataResponse
import com.zakaion.api.service.CloudPaymentModel
import com.zakaion.api.service.CloudPaymentService
import com.zakaion.api.service.TransactionService
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView

@RestController
@CrossOrigin(origins = ["*"], maxAge = 3600)
@RequestMapping(value = ["payment"])
class PaymentController(
        private val userDao: UserDao,
        private val transactionService: TransactionService,
        private val userFactory: UserFactory,
        private val transactionInDao: TransactionInDao,
        private val bankCardDao: BankCardDao,
        private val cloudPaymentService: CloudPaymentService
) : BaseController() {

    @GetMapping("/{userID}/balance")
    fun balance(@PathVariable("userID") userID: Long) : DataResponse<Float> {
        return DataResponse.ok(
                transactionService.userBalance(userID)
        )
    }

    @PostMapping("/{userID}/add/pay")
    fun addPay(@PathVariable("userID") userID: Long,
               @RequestBody addPayModel: AddPayModel) : DataResponse<CloudPaymentModel?> {
        val user = userDao.findById(userID).orElseGet {
            throw NotFound()
        }

        val myUser = userFactory.myUser

        val bankCard = bankCardDao.findById(addPayModel.bankCardID).orElseGet {
            throw NotFound()
        }

        if (bankCard.user.id != myUser.id) {
            throw NoPermittedMethod()
        }

        val makePayment = cloudPaymentService.makePayment(
            "${user.firstName} ${user.lastName}",
            "123.455.244.66",
            bankCard.crypto,
            addPayModel.amount.toInt()
        ) ?: return DataResponse(
            success = false,
            data = null,
            error = "cloud payment null"
        )

        println(Gson().toJson(makePayment))

        if (!makePayment.success) {
            if (makePayment.model?.acsUrl == null ||
                makePayment.model.paReq == null ||
                makePayment.model.transactionId == null)
                return DataResponse(
                    success = false,
                    data = null,
                    error = "cloud payment model null ${makePayment.model == null}"
                )
            return DataResponse.ok(makePayment.model)
        }

        transactionInDao.save(
                TransactionInEntity(
                        amount = addPayModel.amount,
                        user = user,
                        card = bankCard,
                        order = null
                )
        )

        return DataResponse.ok(null)
    }

    @PostMapping("/add/card")
    fun addCard(@RequestBody addCardModel: AddCardModel) : DataResponse<Nothing?> {
        val myUser = userFactory.myUser

        val card = BankCardEntity(
            user = myUser,
            endNum = addCardModel.num,
            crypto = addCardModel.crypto,
            exp = "${addCardModel.expMonth}/${addCardModel.expYear}"
        )

        bankCardDao.save(card)

        return DataResponse.ok(null)
    }

    @GetMapping("/{userID}/card/list")
    fun userCards(pageable: Pageable, @PathVariable("userID") userID: Long) : DataResponse<Page<BankCardEntity>> {
        return DataResponse.ok(
            bankCardDao.userCards(userID, pageable)
        )
    }

    @PostMapping("/{userID}/cloudpayment/3ds/process/{cardID}", produces = [MediaType.TEXT_HTML_VALUE])
    fun processCloudPayment3ds(@RequestParam("MD") transactionId: String,
                                       @RequestParam("PaRes") paRes: String,
                                       @PathVariable("userID") userID: Long,
                                       @PathVariable("cardID") bankCardID: Long): ByteArray {
        val user = userDao.findById(userID).orElseGet {
            throw NotFound()
        }

        val bankCard = bankCardDao.findById(bankCardID).orElseGet {
            throw NotFound()
        }

        val process3ds = cloudPaymentService.process3ds(transactionId, paRes) ?: throw BadParams()

        if (!process3ds.success || process3ds.model?.amount == null)
            return ClassPathResource("templates/fail_payed.html").file.readBytes()

        transactionInDao.save(
            TransactionInEntity(
                amount = process3ds.model.amount,
                user = user,
                card = bankCard,
                order = null
            )
        )

        return ClassPathResource("templates/success_payed.html").file.readBytes()
    }

    @GetMapping("/{userID}/cloudpayment/3ds/{cardID}", produces = [MediaType.TEXT_HTML_VALUE])
    fun getWebCloudPayment3ds(
        @PathVariable("userID") userID: Long,
        @RequestParam("transactionId") transactionId: Int,
        @RequestParam("paReq") paReqN: String,
        @RequestParam("acsUrl") acsUrl: String,
        @PathVariable("cardID") bankCardID: Long
    ): ByteArray {

        val paReq = "+$paReqN"

        val html = "<html>\n" +
                "\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "    <meta name=\"description\" content=\"\">\n" +
                "    <meta name=\"author\" content=\"\">\n" +
                "    <title>3D secure</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<iframe name=\"hidden_iframe\" style=\"position:fixed; top:0; left:0; bottom:0; right:0; width:100%; height:100%; border:none; margin:0; padding:0; overflow:hidden; z-index:999999;\"></iframe>\n" +
                "\n" +
                "<form name=\"downloadForm\" target=\"hidden_iframe\" action=\"$acsUrl\" method=\"POST\">\n" +
                "    <input type=\"hidden\" name=\"PaReq\" value=\"$paReq\">\n" +
                "    <input type=\"hidden\" name=\"MD\" value=\"$transactionId\">\n" +
                "    <input type=\"hidden\" name=\"TermUrl\" value=\"https://api.zakazy.online/api/v1/payment/$userID/cloudpayment/3ds/process/$bankCardID\">\n" +
                "</form>\n" +
                "\n" +
                "<script>\n" +
                "    window.onload = submitForm;\n" +
                "    function submitForm() { downloadForm.submit(); }\n" +
                "</script>\n" +
                "</body>\n" +
                "\n" +
                "</html>"

        return html.toByteArray()
    }

}