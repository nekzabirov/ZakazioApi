// ignore: import_of_legacy_library_into_null_safe
import 'package:rxdart/subjects.dart';
import 'package:zakazy_crm_v2/model/address/CityModel.dart';
import 'package:zakazy_crm_v2/model/order/OrderModel.dart';
import 'package:zakazy_crm_v2/model/unit/PagedListModel.dart';
import 'package:zakazy_crm_v2/repository/OrderRepository.dart';
import 'package:zakazy_crm_v2/repository/UserRepository.dart';
import 'package:zakazy_crm_v2/screen/ZakazyViewModel.dart';

class MyOrderViewModel extends ZakazyViewModel {
  final _orderRepository = OrderRepository();

  final BehaviorSubject<PagedListModel<OrderModel>> orders =
  BehaviorSubject.seeded(PagedListModel(List.empty(), 0));

  final BehaviorSubject<bool> isNewCreate = BehaviorSubject.seeded(false);

  final BehaviorSubject<String?> errorTxt = BehaviorSubject.seeded(null);

  String _searchTxt = '';

  int _cityID = -1;

  MyOrderViewModel();

  search(String txt) {
    _searchTxt = txt;
    load(0);
  }

  setCity(CityModel? cityModel) {
    _cityID = cityModel != null ? cityModel.id : -1;
    load(0);
  }

  load(int pageNum) async {
    orders.add(null);
    final list = await _orderRepository.userOrders(
        UserRepository.instance().myUserLiveData.value!.id,
        pageNum,
        _searchTxt,
        10
    );
    orders.add(list);
  }

  close() {
    orders.close();
    isNewCreate.close();
    errorTxt.close();
  }
}