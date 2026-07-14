package libcore

import (
	"sync"

	tun "github.com/sagernet/sing-tun"
	"github.com/sagernet/sing/common/control"
	E "github.com/sagernet/sing/common/exceptions"
	"github.com/sagernet/sing/common/logger"
	"github.com/sagernet/sing/common/x/list"
)

// platformInterfaceMonitor drives sing-box's default-interface tracking from the
// Android platform. Go's net.Interfaces() is blocked on Android 11+, so the
// NetworkManager's interface list and default interface must be populated via
// the platform (ConnectivityManager). The previous stub never did this, which
// left the dialer's interface list empty ("no available network interface").
type platformInterfaceMonitor struct {
	wrapper *boxPlatformInterfaceWrapper
	logger  logger.Logger

	access       sync.Mutex
	callbacks    list.List[tun.DefaultInterfaceUpdateCallback]
	defaultIf    *control.Interface
	myInterfaces []string
}

func (m *platformInterfaceMonitor) Start() error {
	return m.update()
}

func (m *platformInterfaceMonitor) Close() error {
	return nil
}

// update refreshes the interface list and default interface from the platform,
// then notifies registered callbacks (route/dns default-interface tracking).
func (m *platformInterfaceMonitor) update() error {
	nm := m.wrapper.networkManager
	if nm == nil {
		return nil
	}
	if err := nm.UpdateInterfaces(); err != nil {
		return E.Cause(err, "update interfaces")
	}
	var newDefault *control.Interface
	if index := m.wrapper.getDefaultIndex(); index > 0 {
		if iif, err := nm.InterfaceFinder().ByIndex(index); err == nil {
			newDefault = iif
		}
	}
	m.access.Lock()
	m.defaultIf = newDefault
	callbacks := m.callbacks.Array()
	m.access.Unlock()
	for _, callback := range callbacks {
		callback(newDefault, 0)
	}
	return nil
}

func (m *platformInterfaceMonitor) DefaultInterface() *control.Interface {
	m.access.Lock()
	defer m.access.Unlock()
	return m.defaultIf
}

func (m *platformInterfaceMonitor) OverrideAndroidVPN() bool {
	return false
}

func (m *platformInterfaceMonitor) AndroidVPNEnabled() bool {
	return false
}

func (m *platformInterfaceMonitor) RegisterCallback(callback tun.DefaultInterfaceUpdateCallback) *list.Element[tun.DefaultInterfaceUpdateCallback] {
	m.access.Lock()
	defer m.access.Unlock()
	return m.callbacks.PushBack(callback)
}

func (m *platformInterfaceMonitor) UnregisterCallback(element *list.Element[tun.DefaultInterfaceUpdateCallback]) {
	m.access.Lock()
	defer m.access.Unlock()
	m.callbacks.Remove(element)
}

func (m *platformInterfaceMonitor) RegisterMyInterface(interfaceName string) {
	m.access.Lock()
	defer m.access.Unlock()
	m.myInterfaces = append(m.myInterfaces, interfaceName)
}

func (m *platformInterfaceMonitor) MyInterface() string {
	m.access.Lock()
	defer m.access.Unlock()
	if len(m.myInterfaces) == 0 {
		return ""
	}
	return m.myInterfaces[len(m.myInterfaces)-1]
}

func (m *platformInterfaceMonitor) MyInterfaces() []string {
	m.access.Lock()
	defer m.access.Unlock()
	return append([]string(nil), m.myInterfaces...)
}

var _ tun.DefaultInterfaceMonitor = (*platformInterfaceMonitor)(nil)
