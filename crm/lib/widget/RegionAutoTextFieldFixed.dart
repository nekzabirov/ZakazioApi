import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
// ignore: import_of_legacy_library_into_null_safe
import 'package:rxdart/rxdart.dart';
import 'package:zakazy_crm_v2/conts.dart';
import 'package:zakazy_crm_v2/model/address/CityModel.dart';
import 'package:zakazy_crm_v2/model/address/RegionModel.dart';
import 'package:zakazy_crm_v2/repository/AddressResponse.dart';
// ignore: import_of_legacy_library_into_null_safe
import 'package:flutter_typeahead/flutter_typeahead.dart';
import 'package:zakazy_crm_v2/rest/AddressRest.dart';
import 'package:zakazy_crm_v2/widget/ZTextField.dart';

class RegionAutoTextFieldFixed extends StatefulWidget {
  final Function(RegionModel? cityModel) onSelect;

  RegionAutoTextFieldFixed(this.onSelect);

  @override
  State<StatefulWidget> createState() =>
      _RegionAutoTextFieldFixed(onSelect);
}

class _RegionAutoTextFieldFixed extends State<RegionAutoTextFieldFixed>
    with WidgetsBindingObserver {
  final Function(RegionModel? cityModel) onSelect;
  RegionModel? selectedCity;

  final _addressRepository = AddressResponse();

  //final TextEditingController _typeAheadController = TextEditingController();

  final _list = BehaviorSubject<List<RegionModel>>.seeded(null);

  final FocusNode _focusNode = FocusNode();

  late ZTextField _textField = ZTextField(
      hint: "Регион",
      focusNode: _focusNode,
      txt: selectedCity?.name,
      onEdit: (txt) async {
        _list.add(null);
        final data = await AddressRest().regionsSearch(txt);
        _list.add(data.data!.content);
      });

  OverlayEntry? _overlayEntry;

  bool _isOpenList = false;

  _RegionAutoTextFieldFixed(this.onSelect);

  @override
  void initState() {
    super.initState();
    _focusNode.addListener(() {
      setState(() {
        _isOpenList = _focusNode.hasFocus;
      });
    });
  }

  @override
  Widget build(BuildContext context) => SizedBox(
      width: double.infinity,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [_textField, _isOpenList ? _listData() : Container()],
      ));

  @override
  void dispose() {
    _list.close();
    super.dispose();
  }

  Widget _listData() => Container(
    decoration: BoxDecoration(
        border: Border(
            left: BorderSide(color: primaryColor, width: 2),
            right: BorderSide(color: primaryColor, width: 2),
            bottom: BorderSide(color: primaryColor, width: 2),
            top: BorderSide(color: primaryColor, width: 2)),
        borderRadius: BorderRadius.only(
            bottomLeft: Radius.circular(8),
            bottomRight: Radius.circular(8))
    ),
    child: StreamBuilder<List<RegionModel>>(
        stream: _list,
        builder: (context, snapshot) {
          child() {
            if (!snapshot.hasData)
              return Center(
                child: SizedBox(
                  width: 25,
                  height: 25,
                  child: CircularProgressIndicator(),
                ),
              );
            final data = snapshot.requireData;
            return ListView(
              children: List.generate(data.length + 1, (index) {
                if (index == 0) {
                  return MouseRegion(
                    cursor: SystemMouseCursors.click,
                    child: GestureDetector(
                      onTap: () {
                        this._focusNode.unfocus();
                        this._textField.setText("");
                        this.onSelect(null);
                        // this._focusNode.
                      },
                      child: ListTile(title: Text("Закрыть/Очистить")),
                    ),
                  );
                }
                final cityModel = data[index - 1];
                return MouseRegion(
                  cursor: SystemMouseCursors.click,
                  child: GestureDetector(
                    onTap: () {
                      this._focusNode.unfocus();
                      this._textField.setText(cityModel.name);
                      this.onSelect(cityModel);
                      // this._focusNode.
                    },
                    child: ListTile(
                      title: Text(cityModel.name)
                    ),
                  ),
                );
              }),
            );
          }

          return Container(
            color: Colors.transparent,
            constraints: BoxConstraints(maxHeight: 250),
            child: child(),
          );
        }),
  );
}