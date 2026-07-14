package libcore

import (
	"encoding/json"
	"fmt"
	"libcore/procfs"
	"log"
	"net"
	"net/netip"
	"strings"
	"sync"
	"syscall"

	"github.com/matsuridayo/libneko/neko_log"
	"github.com/sagernet/sing-box/adapter"
	C "github.com/sagernet/sing-box/constant"
	sblog "github.com/sagernet/sing-box/log"
	"github.com/sagernet/sing-box/option"
	tun "github.com/sagernet/sing-tun"
	"github.com/sagernet/sing/common/control"
	E "github.com/sagernet/sing/common/exceptions"
	"github.com/sagernet/sing/common/logger"
	N "github.com/sagernet/sing/common/network"
)

type boxPlatformInterfaceWrapper struct {
	networkManager adapter.NetworkManager
	access         sync.Mutex
	defaultIndex   int
	monitor        *platformInterfaceMonitor
}

func (w *boxPlatformInterfaceWrapper) Initialize(networkManager adapter.NetworkManager) error {
	w.networkManager = networkManager
	return nil
}

func (w *boxPlatformInterfaceWrapper) setDefaultIndex(index int) {
	w.access.Lock()
	w.defaultIndex = index
	w.access.Unlock()
}

func (w *boxPlatformInterfaceWrapper) getDefaultIndex() int {
	w.access.Lock()
	defer w.access.Unlock()
	return w.defaultIndex
}

func (w *boxPlatformInterfaceWrapper) UsePlatformAutoDetectInterfaceControl() bool {
	return true
}

func (w *boxPlatformInterfaceWrapper) AutoDetectInterfaceControl(fd int) error {
	// call protect_path
	if !isBgProcess {
		_ = sendFdToProtect(fd, "protect_path")
		return nil
	}
	// bg process call VPNService
	return intfBox.AutoDetectInterfaceControl(int32(fd))
}

func (w *boxPlatformInterfaceWrapper) UsePlatformInterface() bool {
	return true
}

func (w *boxPlatformInterfaceWrapper) OpenInterface(options *tun.Options, platformOptions option.TunPlatformOptions) (tun.Tun, error) {
	if len(options.IncludeUID) > 0 || len(options.ExcludeUID) > 0 {
		return nil, E.New("android: unsupported uid options")
	}
	if len(options.IncludeAndroidUser) > 0 {
		return nil, E.New("android: unsupported android_user option")
	}
	a, _ := json.Marshal(options)
	b, _ := json.Marshal(platformOptions)
	tunFd, err := intfBox.OpenTun(string(a), string(b))
	if err != nil {
		return nil, fmt.Errorf("intfBox.OpenTun: %v", err)
	}
	// Do you want to close it?
	tunFd, err = syscall.Dup(tunFd)
	if err != nil {
		return nil, fmt.Errorf("syscall.Dup: %v", err)
	}
	//
	options.FileDescriptor = int(tunFd)
	return tun.New(*options)
}

func (w *boxPlatformInterfaceWrapper) UsePlatformDefaultInterfaceMonitor() bool {
	return true
}

func (w *boxPlatformInterfaceWrapper) CreateDefaultInterfaceMonitor(l logger.Logger) tun.DefaultInterfaceMonitor {
	m := &platformInterfaceMonitor{wrapper: w, logger: l}
	w.monitor = m
	return m
}

func (w *boxPlatformInterfaceWrapper) UsePlatformNetworkInterfaces() bool {
	// Go's net.Interfaces() is blocked on Android 11+, so the dialer's
	// interface enumeration comes from the platform (ConnectivityManager).
	return true
}

func (w *boxPlatformInterfaceWrapper) NetworkInterfaces() ([]adapter.NetworkInterface, error) {
	raw, err := intfBox.GetInterfaces()
	if err != nil {
		return nil, err
	}
	var list []struct {
		Name      string   `json:"name"`
		Index     int      `json:"index"`
		MTU       int      `json:"mtu"`
		Addresses []string `json:"addresses"`
		Type      int      `json:"type"`
		Metered   bool     `json:"metered"`
		Default   bool     `json:"default"`
	}
	if err := json.Unmarshal([]byte(raw), &list); err != nil {
		return nil, err
	}
	interfaces := make([]adapter.NetworkInterface, 0, len(list))
	defaultIndex := 0
	for _, it := range list {
		var prefixes []netip.Prefix
		for _, a := range it.Addresses {
			if p, e := netip.ParsePrefix(a); e == nil {
				prefixes = append(prefixes, p)
			}
		}
		if it.Default {
			defaultIndex = it.Index
		}
		interfaces = append(interfaces, adapter.NetworkInterface{
			Interface: control.Interface{
				Index:     it.Index,
				MTU:       it.MTU,
				Name:      it.Name,
				Addresses: prefixes,
				Flags:     net.FlagUp | net.FlagRunning,
			},
			Type:      C.InterfaceType(it.Type),
			Expensive: it.Metered,
		})
	}
	w.setDefaultIndex(defaultIndex)
	return interfaces, nil
}

func (w *boxPlatformInterfaceWrapper) UnderNetworkExtension() bool {
	return false
}

func (w *boxPlatformInterfaceWrapper) NetworkExtensionIncludeAllNetworks() bool {
	return false
}

func (w *boxPlatformInterfaceWrapper) ClearDNSCache() {
}

func (w *boxPlatformInterfaceWrapper) RequestPermissionForWIFIState() error {
	return nil
}

func (w *boxPlatformInterfaceWrapper) ReadWIFIState() adapter.WIFIState {
	state := strings.Split(intfBox.WIFIState(), ",")
	if len(state) < 2 {
		return adapter.WIFIState{}
	}
	return adapter.WIFIState{
		SSID:  state[0],
		BSSID: state[1],
	}
}

func (w *boxPlatformInterfaceWrapper) SystemCertificates() []string {
	return nil
}

func (w *boxPlatformInterfaceWrapper) UsePlatformConnectionOwnerFinder() bool {
	return true
}

func (w *boxPlatformInterfaceWrapper) FindConnectionOwner(request *adapter.FindConnectionOwnerRequest) (*adapter.ConnectionOwner, error) {
	var uid int32
	if useProcfs {
		var network string
		switch request.IpProtocol {
		case syscall.IPPROTO_TCP:
			network = N.NetworkTCP
		case syscall.IPPROTO_UDP:
			network = N.NetworkUDP
		default:
			return nil, E.New("unknown ip protocol: ", request.IpProtocol)
		}
		sourceAddr, _ := netip.ParseAddr(request.SourceAddress)
		destinationAddr, _ := netip.ParseAddr(request.DestinationAddress)
		source := netip.AddrPortFrom(sourceAddr, uint16(request.SourcePort))
		destination := netip.AddrPortFrom(destinationAddr, uint16(request.DestinationPort))
		uid = procfs.ResolveSocketByProcSearch(network, source, destination)
		if uid == -1 {
			return nil, E.New("procfs: not found")
		}
	} else {
		var err error
		uid, err = intfBox.FindConnectionOwner(request.IpProtocol, request.SourceAddress, request.SourcePort, request.DestinationAddress, request.DestinationPort)
		if err != nil {
			return nil, err
		}
	}
	packageName, _ := intfBox.PackageNameByUid(uid)
	return &adapter.ConnectionOwner{
		UserId:              uid,
		AndroidPackageNames: []string{packageName},
	}, nil
}

func (w *boxPlatformInterfaceWrapper) UsePlatformWIFIMonitor() bool {
	return false
}

func (w *boxPlatformInterfaceWrapper) UsePlatformNotification() bool {
	return false
}

func (w *boxPlatformInterfaceWrapper) SendNotification(notification *adapter.Notification) error {
	return nil
}

func (w *boxPlatformInterfaceWrapper) MyInterfaceAddress() []netip.Addr {
	return nil
}

// io.Writer

var disableSingBoxLog = false

func (w *boxPlatformInterfaceWrapper) Write(p []byte) (n int, err error) {
	// use neko_log
	if !disableSingBoxLog {
		log.Print(string(p))
	}
	return len(p), nil
}

// 日志

type boxPlatformLogWriterWrapper struct {
}

var boxPlatformLogWriter sblog.PlatformWriter = &boxPlatformLogWriterWrapper{}

func (w *boxPlatformLogWriterWrapper) DisableColors() bool { return true }

func (w *boxPlatformLogWriterWrapper) WriteMessage(level uint8, message string) {
	if !strings.HasSuffix(message, "\n") {
		message += "\n"
	}
	neko_log.LogWriter.Write([]byte(message))
}
