import 'package:flutter/material.dart';
import 'package:zakazy_crm_v2/model/settings/notification_enable_settings.dart';
import 'package:zakazy_crm_v2/model/settings/primary_settings.dart';
import 'package:zakazy_crm_v2/screen/HomeScreen.dart';
import 'package:zakazy_crm_v2/screen/settings/SettingsFullVM.dart';
import 'package:zakazy_crm_v2/screen/settings/edit/settings_edit_notification_enables.dart';
import 'package:zakazy_crm_v2/screen/settings/edit/settings_edit_primary.dart';
import 'package:zakazy_crm_v2/widget/payment/OrderTransactionList.dart';

class SettingsFullScreen extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => SettingsFullState();
}

class SettingsFullState
    extends HomeScreen<SettingsFullScreen, SettingsFullViewModel> {
  @override
  Widget rightDialog() => StreamBuilder(
      stream: viewModel().onEdit,
      builder: (_, snapShot) {
        if (!snapShot.hasData) return Container();

        if (snapShot.data == SettingsEditType.PRIMARY)
          return SettingseditPrimary();
        else if (snapShot.data == SettingsEditType.NOTIFICATION_ENABLES)
          return SettingsEditNotificationEnables();
        else
          return Container();
      });

  @override
  Widget body() => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [primaryBlock(), notitcationEnablesBlock()],
      );

  primaryBlock() {
    viewModel().loadPrimary();
    return Card(
        elevation: 4,
        child: SizedBox(
            width: double.infinity,
            child: Stack(
              alignment: Alignment.bottomRight,
              children: [
                Container(
                  constraints: BoxConstraints(minHeight: 200),
                  child: Padding(
                    padding: EdgeInsets.all(24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          "Аукцион",
                          style: TextStyle(
                              fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        Divider(
                          height: 25,
                          color: Colors.transparent,
                        ),
                        StreamBuilder<PrimarySettings>(
                            stream: viewModel().primary,
                            builder: (_, snapShot) {
                              if (!snapShot.hasData) {
                                return Center(
                                    child: CircularProgressIndicator());
                              }
                              final data = snapShot.requireData;

                              return Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    "Комисия за заказ у исполнителя:",
                                    style: TextStyle(fontSize: 16),
                                  ),
                                  Divider(
                                    height: 5,
                                    color: Colors.transparent,
                                  ),
                                  Text("${data.orderSumOutPercent}%"),
                                  Divider(
                                    height: 25,
                                    color: Colors.transparent,
                                  ),
                                  Text(
                                    "Вознограждение от полученой комисии:",
                                    style: TextStyle(fontSize: 16),
                                  ),
                                  Divider(
                                    height: 10,
                                    color: Colors.transparent,
                                  ),
                                  SizedBox(
                                    width: double.infinity,
                                    child: DataTable(
                                      columns: [
                                        BaseColumn("Роль в заказе"),
                                        BaseColumn("Процент")
                                      ],
                                      rows: [
                                        primarySetRow("Партнер заказа",
                                            data.orderPartnerPercent),
                                        primarySetRow("Агент исполнителя",
                                            data.executorPartnerPercent)
                                      ],
                                    ),
                                  )
                                ],
                              );
                            })
                      ],
                    ),
                  ),
                ),
                Padding(
                  padding: EdgeInsets.all(24),
                  child: FloatingActionButton(
                    onPressed: () =>
                        {viewModel().onEdit.add(SettingsEditType.PRIMARY)},
                    child: Icon(Icons.edit),
                  ),
                )
              ],
            )));
  }

  notitcationEnablesBlock() {
    viewModel().loadNotificationEnable();
    return Card(
        elevation: 4,
        child: SizedBox(
            width: double.infinity,
            child: Stack(
              alignment: Alignment.bottomRight,
              children: [
                Container(
                  constraints: BoxConstraints(minHeight: 200),
                  child: Padding(
                    padding: EdgeInsets.all(24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          "Уведомитель",
                          style: TextStyle(
                              fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        Divider(
                          height: 25,
                          color: Colors.transparent,
                        ),
                        StreamBuilder<
                                MapEntry<NotificationEnableSettings,
                                    NotificationEnableSettings>>(
                            stream: viewModel().notificationEnable,
                            builder: (_, snapShot) {
                              if (!snapShot.hasData) {
                                return Center(
                                    child: CircularProgressIndicator());
                              }
                              final data = snapShot.requireData;

                              return SizedBox(
                                width: double.infinity,
                                child: DataTable(
                                  columns: [
                                    BaseColumn("Уведомлять о"),
                                    BaseColumn("Отправлять по email"),
                                    BaseColumn("Отправлять по смс")
                                  ],
                                  rows: [
                                    notificationEnableSetRow(
                                        "Создан заказ с вашим учатьем",
                                        data.key.createOrder,
                                        data.value.createOrder),
                                    notificationEnableSetRow(
                                        "Стал/Назначили исполнителем",
                                        data.key.youExecutorOrder,
                                        data.value.youExecutorOrder),
                                    notificationEnableSetRow(
                                        "Заказ получил исполнителя",
                                        data.key.clientHasExecutor,
                                        data.value.clientHasExecutor),
                                    notificationEnableSetRow(
                                        "Изменился статус заказа",
                                        data.key.clientOrderInWork,
                                        data.value.clientOrderInWork)
                                  ],
                                ),
                              );
                            })
                      ],
                    ),
                  ),
                ),
                Padding(
                  padding: EdgeInsets.all(24),
                  child: FloatingActionButton(
                    onPressed: () => {
                      viewModel()
                          .onEdit
                          .add(SettingsEditType.NOTIFICATION_ENABLES)
                    },
                    child: Icon(Icons.edit),
                  ),
                )
              ],
            )));
  }

  @override
  Widget top() => createTitleView("Настройки");

  @override
  SettingsFullViewModel viewModelInstancer() => SettingsFullViewModel();

  primarySetRow(String role, int procent) =>
      DataRow(cells: [BaseCell(role), BaseCell("$procent%")]);

  notificationEnableSetRow(String topic, bool email, bool sms) =>
      DataRow(cells: [
        BaseCell(topic),
        BaseCell(email ? "Включенно" : "Отключенно"),
        BaseCell(sms ? "Включенно" : "Отключенно")
      ]);
}