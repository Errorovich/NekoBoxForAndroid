//go:build with_naive_outbound

package libcore

import (
	"github.com/sagernet/sing-box/protocol/naive"
)

// Enabled only with the with_naive_outbound build tag. The naive outbound
// statically links Chromium cronet (libcronet.a); the linked code is ~5-9 MB
// per ABI after stripping.
func init() {
	registerNaiveOutbound = naive.RegisterOutbound
}
