package com.zakaion.api.controller.user

import com.zakaion.api.controller.BaseController
import com.zakaion.api.dao.UserDao
import com.zakaion.api.entity.user.RoleType
import com.zakaion.api.entity.user.UserEntity
import com.zakaion.api.exception.AlreadyTaken
import com.zakaion.api.exception.NoPermittedMethod
import com.zakaion.api.exception.NotFound
import com.zakaion.api.exception.WrongPassword
import com.zakaion.api.model.DataResponse
import com.zakaion.api.model.PhoneRegister
import com.zakaion.api.model.TokenModel
import com.zakaion.api.roleControllers.CanSuperAdmin_Admin_Editor
import com.zakaion.api.roleControllers.CanSuperAdmin_Admin_Editor_Partner
import com.zakaion.api.service.AuthTokenService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(value = ["client"])
class ClientController (private val userDao: UserDao,
                        private val authTokenService: AuthTokenService,
                        private val userController: UserController) : BaseController(){

    @GetMapping("/list")
    @CanSuperAdmin_Admin_Editor_Partner
    fun list(pageable: Pageable) : DataResponse<Page<UserEntity>> {

        val data = userDao.findByRole(RoleType.CLIENT.ordinal, pageable)

        return DataResponse.ok(
                data
        )
    }

    @PostMapping("/register/phone")
    fun registerPhone(@RequestBody phoneRegister: PhoneRegister) : DataResponse<TokenModel> {
        if (userDao.findAll().any { it.phoneNumber == phoneRegister.phoneNumber })
            throw AlreadyTaken()

        if (phoneRegister.token != null && phoneRegister.smsCode != null) {
            val phoneSms = authTokenService.parsePhoneToken(phoneRegister.token) ?: throw WrongPassword()
            if (phoneSms.second != phoneRegister.smsCode) throw WrongPassword()

            return DataResponse.ok(
                    TokenModel(
                            authTokenService.generatePhoneToken(phoneRegister.phoneNumber, "null")
                    )
            )
        }

        return DataResponse.ok(
                TokenModel(
                        authTokenService.generatePhoneToken(phoneRegister.phoneNumber, "1234")
                )
        )
    }

    @PostMapping("/register")
    fun register(@RequestBody userEntity: UserEntity,
                 @RequestHeader("token") token: String) : DataResponse<TokenModel> {

        val phoneNumber = authTokenService.parsePhoneToken(token)?.first ?: throw WrongPassword()

        if (userDao.findAll().any { it.phoneNumber == phoneNumber || it.email == userEntity.email }) {
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
    @CanSuperAdmin_Admin_Editor
    fun delete(@PathVariable("id") id: Long): DataResponse<Nothing?> {

        val user = userDao.findById(id).orElseGet { throw NotFound() }
        if (user.role != RoleType.CLIENT) throw NotFound()

        userDao.delete(user)

        return DataResponse.ok(null)
    }

    @PostMapping("/add")
    @CanSuperAdmin_Admin_Editor_Partner
    fun add(@RequestBody userEntity: UserEntity): DataResponse<UserEntity> {

        if (userDao.findAll().any { it.phoneNumber == userEntity.phoneNumber || it.email == userEntity.email }) {
            throw AlreadyTaken()
        }

        return DataResponse.ok(
                userDao.save(
                        userEntity.copy(
                                role = RoleType.CLIENT
                        )
                )
        )
    }
}