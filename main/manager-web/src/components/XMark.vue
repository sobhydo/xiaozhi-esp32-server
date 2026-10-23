<template>
  <svg
    class="x-mark"
    :width="size"
    :height="size"
    viewBox="0 0 100 100"
    :role="label ? 'img' : null"
    :aria-label="label || null"
    :aria-hidden="label ? null : 'true'"
  >
    <template v-if="inverse">
      <rect x="3" y="38.5" width="94" height="23" rx="11.5" transform="rotate(45 50 50)" fill="none" stroke="#ffffff" stroke-width="5" />
      <rect x="1.5" y="37.3" width="97" height="25.4" rx="12.7" transform="rotate(-45 50 50)" fill="#ffffff" />
    </template>
    <template v-else>
      <rect x="1.5" y="37.3" width="97" height="25.4" rx="12.7" transform="rotate(45 50 50)" fill="#18181b" />
      <rect x="4.5" y="40" width="91" height="20" rx="10" transform="rotate(-45 50 50)" fill="#ffffff" stroke="#18181b" stroke-width="7" />
    </template>
  </svg>
</template>

<script>
// The X brand mark: one filled bar crossed by one outlined bar.
// `inverse` draws the white-on-dark version.
export default {
  name: "XMark",
  props: {
    size: { type: [Number, String], default: 20 },
    inverse: { type: Boolean, default: false },
    label: { type: String, default: "" },
  },
};
</script>
