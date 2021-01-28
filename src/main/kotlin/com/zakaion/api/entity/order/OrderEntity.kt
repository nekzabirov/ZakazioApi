package com.zakaion.api.entity.order

import com.zakaion.api.entity.region.CityEntity
import com.zakaion.api.entity.user.UserEntity
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.util.*
import javax.persistence.*

@Entity(name = "order_n")
//@Table(name = "order_n")
data class OrderEntity(
        @Id
        @GeneratedValue(strategy= GenerationType.AUTO)
        val id: Long = 0L,
        @Enumerated val status: OrderStatus = OrderStatus.PROCESS,

        @OneToOne
        //@JoinColumn(name = "client_id")
        val client: UserEntity,

        @OneToOne
        //@JoinColumn(name = "app_id")
        val app: AppEntity? = null,

        @OneToOne
        //@JoinColumn(name = "partner_id")
        val partner: UserEntity? = null,

        @OneToOne
        //@JoinColumn(name = "executor_id")
        var executor: UserEntity? = null,

        val title: String,

        @Lob
        @Column(name = "content", length = 1024)
        val content: String,

        val price: Float,
        val dateLine: String,

        @OneToOne
       // @JoinColumn(name = "city_id")
        val city: CityEntity,

        @CreationTimestamp
        @Column(name = "creation_date_time", columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
        val creationDateTime: Date = Date(),
        @UpdateTimestamp
        @Column(name = "modified_date_time", columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
        val modifiedDateTime: Date = Date(),

        @ElementCollection
        val files: List<String>,

        @OneToOne
        val category: CategoryEntity
)

enum class OrderStatus(val data: String) {
    PROCESS("process"),
    IN_WORK("in_work"),
    DONE("done"),
    CANCEL("cancel")
}