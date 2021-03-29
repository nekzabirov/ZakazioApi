package com.zakaion.api.controller.user

import com.zakaion.api.controller.BaseController
import com.zakaion.api.dao.UserDao
import com.zakaion.api.entity.user.RoleType
import com.zakaion.api.entity.user.UserEntity
import com.zakaion.api.entity.user._Can_SuperAdmin_Admin_Editor
import com.zakaion.api.entity.user._Can_SuperAdmin_Admin_Editor_Partner
import com.zakaion.api.exception.AlreadyTaken
import com.zakaion.api.exception.BadParams
import com.zakaion.api.exception.NotFound
import com.zakaion.api.exception.WrongPassword
import com.zakaion.api.factor.user.UserFactory
import com.zakaion.api.model.*
import com.zakaion.api.service.AuthTokenService
import com.zakaion.api.service.SmsService
import com.zakaion.api.service.StorageService
import com.zakaion.api.unit.ImportExcellService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(value = ["client"])
class ClientController (private val userDao: UserDao,
                        private val authTokenService: AuthTokenService,
                        private val userController: UserController,
                        private val smsService: SmsService,
                        private val userFactory: UserFactory,
                        private val storageService: StorageService,
                        private val importExcellService: ImportExcellService) : BaseController(){

    @GetMapping("/list")
    @PreAuthorize(_Can_SuperAdmin_Admin_Editor_Partner)
    fun list(pageable: Pageable, @RequestParam("search", required = false, defaultValue = "") search: String? = null) : DataResponse<Page<ClientInfo>> {
        val myUser = userFactory.myUser

        val data = if (myUser.role == RoleType.PARTNER) {
            (
                    if (search.isNullOrEmpty()) userDao.findByRole(RoleType.CLIENT.ordinal, myUser.id, pageable)
                    else userDao.findByRole(RoleType.CLIENT.ordinal, myUser.id, search, pageable)
                    )
        }
                else {
            (
                    if (search.isNullOrEmpty()) userDao.findByRole(RoleType.CLIENT.ordinal, pageable)
                    else userDao.findByRole(RoleType.CLIENT.ordinal, search, pageable)
                    )
        }
            .map {user->
                    userFactory.create(user) as ClientInfo
                }

        return DataResponse.ok(
                data
        )
    }

    @PostMapping("/register/phone")
    fun registerPhone(@RequestBody phoneRegister: PhoneRegister) : DataResponse<TokenModel> {
        if (userDao.findAll().any { it.phoneNumber == phoneRegister.phoneNumber && it.password.isNotEmpty() })
            throw AlreadyTaken()

        if (phoneRegister.token != null && phoneRegister.smsCode != null) {
            val phoneSms = authTokenService.parsePhoneToken(phoneRegister.token) ?: throw WrongPassword()
            if (phoneSms.second != phoneRegister.smsCode) throw WrongPassword()

            return DataResponse.ok(
                    TokenModel(
                            authTokenService.generatePhoneToken(phoneRegister.phoneNumber!!, "null")
                    )
            )
        }

        val code = "1234"

        smsService.sendCode(phoneNumber = phoneRegister.phoneNumber!!, code = code)

        return DataResponse.ok(
                TokenModel(
                        authTokenService.generatePhoneToken(phoneRegister.phoneNumber, code)
                )
        )
    }

    @PostMapping("/register")
    fun register(@RequestBody userEntity: UserEntity,
                 @RequestHeader("token") token: String) : DataResponse<TokenModel> {

        val phoneNumber = authTokenService.parsePhoneToken(token)?.first ?: throw WrongPassword()

        if (userDao.findAll().any { (it.phoneNumber == phoneNumber || it.email == userEntity.email) && it.password.isNotEmpty()}) {
            throw AlreadyTaken()
        }

        var user = userEntity.copy(
                phoneNumber = phoneNumber,
                role = RoleType.CLIENT,
                isPhoneActive = true
        )

        user = userDao.save(user)

        return DataResponse.ok(
                TokenModel(
                        authTokenService.generateToken(user)
                )
        )
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(_Can_SuperAdmin_Admin_Editor)
    fun delete(@PathVariable("id") id: Long): DataResponse<Nothing?> {

        val user = userDao.findById(id).orElseGet { throw NotFound() }
        if (user.role != RoleType.CLIENT) throw NotFound()

        userDao.delete(user)

        return DataResponse.ok(null)
    }

    @PostMapping("/add")
    @PreAuthorize(_Can_SuperAdmin_Admin_Editor_Partner)
    fun add(@RequestBody userEntity: UserEntity): DataResponse<UserEntity> {
        val myUser = userFactory.myUser

        if (userEntity.phoneNumber.isNullOrEmpty() || userEntity.email.isNullOrEmpty())
            throw BadParams()

        if (userDao.findAll().any { it.phoneNumber == userEntity.phoneNumber || it.email == userEntity.email }) {
            throw AlreadyTaken()
        }

        return DataResponse.ok(
                userDao.save(
                        userEntity.copy(
                                role = RoleType.CLIENT
                        ).apply {
                            if (myUser.role == RoleType.PARTNER) {
                                this.masterID = myUser.id
                            }
                        }
                )
        )
    }

    @PostMapping("/import/{filename:.+}")
    suspend fun import(@PathVariable filename: String) : DataResponse<Nothing?> = withContext(Dispatchers.IO) {
        val inputStream = storageService.loadAsFile(filename).inputStream()

        importExcellService.processUser(inputStream, RoleType.CLIENT)

        return@withContext DataResponse.ok(null)
    }
}